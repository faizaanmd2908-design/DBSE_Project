require('dotenv').config({ path: require('path').resolve(__dirname, '../../../.env') });
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { body, validationResult } = require('express-validator');
const mysql = require('mysql2/promise');

const app = express();
const PORT = process.env.AUTH_PORT || 3001;

const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '3306'),
  user: process.env.DB_USER || 'hestia_user',
  password: process.env.DB_PASSWORD || 'hestia_pass',
  database: process.env.DB_NAME || 'hestia_db',
  waitForConnections: true,
  connectionLimit: 10
});

app.use(helmet());
app.use(cors({ origin: process.env.CORS_ORIGIN || '*' }));
app.use(express.json());
app.use(morgan('dev'));

app.get('/health', (req, res) => res.json({ status: 'ok', service: 'auth-service' }));

// Register
app.post('/auth/register',
  body('name').trim().isLength({ min: 2 }).withMessage('Name must be at least 2 characters'),
  body('email').isEmail().normalizeEmail().withMessage('Valid email required'),
  body('password').isLength({ min: 6 }).withMessage('Password must be at least 6 characters'),
  body('phone').optional().isMobilePhone('any'),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }
    const { name, email, password, phone } = req.body;
    try {
      const [existing] = await pool.query('SELECT user_id FROM users WHERE email = ?', [email]);
      if (existing.length > 0) {
        return res.status(409).json({ success: false, message: 'Email already registered' });
      }
      const hash = await bcrypt.hash(password, 10);
      const [result] = await pool.query(
        'INSERT INTO users (name, email, phone, password_hash, role) VALUES (?, ?, ?, ?, ?)',
        [name, email, phone || null, hash, 'BUYER']
      );
      const userId = result.insertId;
      await pool.query(
        'INSERT INTO activity_history (user_id, activity_type, activity_details) VALUES (?, ?, ?)',
        [userId, 'VIEWED', 'registered']
      );
      res.status(201).json({
        success: true,
        message: 'Registration successful',
        data: { id: userId, name, email, role: 'BUYER' }
      });
    } catch (err) {
      console.error('Register error:', err);
      res.status(500).json({ success: false, message: 'Registration failed' });
    }
  }
);

// Login
app.post('/auth/login',
  body('email').isEmail().normalizeEmail(),
  body('password').notEmpty(),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }
    const { email, password } = req.body;
    try {
      const [rows] = await pool.query(
        'SELECT user_id AS id, name, email, phone, password_hash, role FROM users WHERE email = ?',
        [email]
      );
      if (rows.length === 0) {
        return res.status(401).json({ success: false, message: 'Invalid email or password' });
      }
      const user = rows[0];
      const match = await bcrypt.compare(password, user.password_hash);
      if (!match) {
        return res.status(401).json({ success: false, message: 'Invalid email or password' });
      }
      const token = jwt.sign(
        { id: user.id, email: user.email, role: user.role, name: user.name },
        process.env.JWT_SECRET || 'dev_secret_change_me',
        { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
      );
      await pool.query(
        'INSERT INTO activity_history (user_id, activity_type, activity_details) VALUES (?, ?, ?)',
        [user.id, 'VIEWED', 'login']
      );
      res.json({
        success: true,
        message: 'Login successful',
        data: {
          token,
          user: {
            id: user.id,
            name: user.name,
            email: user.email,
            phone: user.phone,
            role: user.role,
          }
        }
      });
    } catch (err) {
      console.error('Login error:', err);
      res.status(500).json({ success: false, message: 'Login failed' });
    }
  }
);

// Me
app.get('/auth/me', async (req, res) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: 'Authentication required' });
  }
  try {
    const decoded = jwt.verify(authHeader.split(' ')[1], process.env.JWT_SECRET || 'dev_secret_change_me');
    const [rows] = await pool.query(
      'SELECT user_id AS id, name, email, phone, role, created_at FROM users WHERE user_id = ?',
      [decoded.id]
    );
    if (rows.length === 0) {
      return res.status(404).json({ success: false, message: 'User not found' });
    }
    res.json({ success: true, data: rows[0] });
  } catch (err) {
    res.status(401).json({ success: false, message: 'Invalid or expired token' });
  }
});

// Update profile
app.put('/auth/me', async (req, res) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: 'Authentication required' });
  }
  try {
    const decoded = jwt.verify(authHeader.split(' ')[1], process.env.JWT_SECRET || 'dev_secret_change_me');
    const { name, phone } = req.body;
    await pool.query('UPDATE users SET name = COALESCE(?, name), phone = COALESCE(?, phone) WHERE user_id = ?',
      [name, phone, decoded.id]);
    await pool.query(
      'INSERT INTO activity_history (user_id, activity_type, activity_details) VALUES (?, ?, ?)',
      [decoded.id, 'VIEWED', JSON.stringify({ name, phone })]
    );
    const [rows] = await pool.query(
      'SELECT user_id AS id, name, email, phone, role FROM users WHERE user_id = ?',
      [decoded.id]
    );
    res.json({ success: true, data: rows[0] });
  } catch (err) {
    res.status(401).json({ success: false, message: 'Invalid token' });
  }
});

// List buyers (admin)
app.get('/auth/users', async (req, res) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: 'Authentication required' });
  }
  try {
    const decoded = jwt.verify(authHeader.split(' ')[1], process.env.JWT_SECRET || 'dev_secret_change_me');
    if (decoded.role !== 'ADMIN') {
      return res.status(403).json({ success: false, message: 'Admin access required' });
    }
    const [rows] = await pool.query(
      "SELECT user_id AS id, name, email, phone, role, created_at FROM users WHERE role = 'BUYER' ORDER BY created_at DESC"
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    res.status(401).json({ success: false, message: 'Invalid token' });
  }
});

app.listen(PORT, () => {
  console.log(`Auth service running on port ${PORT}`);
});
