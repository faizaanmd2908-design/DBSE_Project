require('dotenv').config({ path: require('path').resolve(__dirname, '../../../.env') });
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const jwt = require('jsonwebtoken');
const { body, validationResult } = require('express-validator');
const mysql = require('mysql2/promise');

const app = express();
const PORT = Number(process.env.INTERACTION_PORT || 3003);

const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'hestia_db',
  waitForConnections: true,
  connectionLimit: 10,
  decimalNumbers: true
});

app.use(helmet());
app.use(cors({ origin: process.env.CORS_ORIGIN || '*' }));
app.use(express.json({ limit: '1mb' }));
app.use(morgan('dev'));

function authenticate(req, res, next) {
  const header = req.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: 'Authentication required' });
  }
  try {
    req.user = jwt.verify(header.slice(7), process.env.JWT_SECRET || 'dev_secret_change_me');
    next();
  } catch {
    return res.status(401).json({ success: false, message: 'Invalid or expired token' });
  }
}

function requireAdmin(req, res, next) {
  if (!req.user || req.user.role !== 'ADMIN') {
    return res.status(403).json({ success: false, message: 'Admin access required' });
  }
  next();
}

app.get('/health', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ status: 'ok', service: 'interaction-service', database: 'ok' });
  } catch {
    res.status(503).json({ status: 'degraded', service: 'interaction-service', database: 'unavailable' });
  }
});

// Favourites
app.get('/favourites', authenticate, async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT
        f.favourite_id,
        f.created_at AS favourited_at,
        p.property_id AS id,
        p.property_id,
        p.title,
        p.description,
        p.property_type,
        p.location,
        p.city,
        p.state,
        p.pincode,
        p.price,
        p.area_sqft,
        p.bedrooms,
        p.bathrooms,
        p.status,
        (
          SELECT image_url FROM property_images pi
          WHERE pi.property_id = p.property_id AND pi.is_primary = 1
          ORDER BY pi.image_id LIMIT 1
        ) AS primary_image
       FROM favourites f
       JOIN properties p ON p.property_id = f.property_id
       WHERE f.user_id = ?
       ORDER BY f.created_at DESC`,
      [req.user.id]
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error('Fetch favourites error:', err);
    res.status(500).json({ success: false, message: 'Failed to fetch favourites' });
  }
});

app.post('/favourites', authenticate,
  body('property_id').isInt({ min: 1 }),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ success: false, errors: errors.array() });

    const propertyId = Number(req.body.property_id);
    try {
      const [property] = await pool.query('SELECT property_id FROM properties WHERE property_id = ?', [propertyId]);
      if (property.length === 0) return res.status(404).json({ success: false, message: 'Property not found' });

      const [existing] = await pool.query(
        'SELECT favourite_id FROM favourites WHERE user_id = ? AND property_id = ?',
        [req.user.id, propertyId]
      );
      if (existing.length > 0) {
        return res.status(409).json({ success: false, message: 'Already in favourites' });
      }

      await pool.query('INSERT INTO favourites (user_id, property_id) VALUES (?, ?)', [req.user.id, propertyId]);
      await pool.query(
        'INSERT INTO activity_history (user_id, property_id, activity_type, activity_details) VALUES (?, ?, ?, ?)',
        [req.user.id, propertyId, 'SAVED', 'Added property to favourites']
      );
      res.status(201).json({ success: true, message: 'Added to favourites' });
    } catch (err) {
      console.error('Add favourite error:', err);
      res.status(500).json({ success: false, message: 'Failed to add favourite' });
    }
  }
);

app.delete('/favourites/:propertyId', authenticate, async (req, res) => {
  try {
    const propertyId = Number.parseInt(req.params.propertyId, 10);
    const [result] = await pool.query(
      'DELETE FROM favourites WHERE user_id = ? AND property_id = ?',
      [req.user.id, propertyId]
    );
    if (result.affectedRows === 0) return res.status(404).json({ success: false, message: 'Favourite not found' });

    await pool.query(
      'INSERT INTO activity_history (user_id, property_id, activity_type, activity_details) VALUES (?, ?, ?, ?)',
      [req.user.id, propertyId, 'UNSAVED', 'Removed property from favourites']
    );
    res.json({ success: true, message: 'Removed from favourites' });
  } catch (err) {
    console.error('Remove favourite error:', err);
    res.status(500).json({ success: false, message: 'Failed to remove favourite' });
  }
});

// Interests
app.get('/interests', authenticate, async (req, res) => {
  try {
    let sql;
    let params = [];

    if (req.user.role === 'ADMIN') {
      sql = `SELECT
        i.interest_id AS id,
        i.interest_id,
        i.user_id,
        i.property_id,
        i.status,
        i.created_at,
        i.updated_at,
        u.name AS user_name,
        u.email AS user_email,
        p.title AS property_title,
        p.location,
        p.price
       FROM interests i
       JOIN users u ON u.user_id = i.user_id
       JOIN properties p ON p.property_id = i.property_id
       ORDER BY i.created_at DESC`;
    } else {
      sql = `SELECT
        i.interest_id AS id,
        i.interest_id,
        i.property_id,
        i.status,
        i.created_at,
        i.updated_at,
        p.title AS property_title,
        p.location,
        p.price,
        (
          SELECT image_url FROM property_images pi
          WHERE pi.property_id = p.property_id AND pi.is_primary = 1
          ORDER BY pi.image_id LIMIT 1
        ) AS primary_image
       FROM interests i
       JOIN properties p ON p.property_id = i.property_id
       WHERE i.user_id = ?
       ORDER BY i.created_at DESC`;
      params = [req.user.id];
    }

    const [rows] = await pool.query(sql, params);
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error('Fetch interests error:', err);
    res.status(500).json({ success: false, message: 'Failed to fetch interests' });
  }
});

app.post('/interests', authenticate,
  body('property_id').isInt({ min: 1 }),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ success: false, errors: errors.array() });

    const propertyId = Number(req.body.property_id);
    try {
      const [property] = await pool.query('SELECT property_id FROM properties WHERE property_id = ?', [propertyId]);
      if (property.length === 0) return res.status(404).json({ success: false, message: 'Property not found' });

      const [existing] = await pool.query(
        'SELECT interest_id FROM interests WHERE user_id = ? AND property_id = ?',
        [req.user.id, propertyId]
      );
      if (existing.length > 0) {
        return res.status(409).json({ success: false, message: 'Interest already expressed' });
      }

      const [result] = await pool.query(
        'INSERT INTO interests (user_id, property_id, status) VALUES (?, ?, ?)',
        [req.user.id, propertyId, 'EXPRESSED']
      );

      await pool.query(
        'INSERT INTO activity_history (user_id, property_id, activity_type, activity_details) VALUES (?, ?, ?, ?)',
        [req.user.id, propertyId, 'INTERESTED', 'Expressed interest in property']
      );

      res.status(201).json({
        success: true,
        message: 'Interest expressed',
        data: { id: result.insertId }
      });
    } catch (err) {
      console.error('Express interest error:', err);
      res.status(500).json({ success: false, message: 'Failed to express interest' });
    }
  }
);

app.patch('/interests/:id/status', authenticate, requireAdmin,
  body('status').isIn(['EXPRESSED', 'CONTACTED', 'CLOSED', 'WITHDRAWN']),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ success: false, errors: errors.array() });
    try {
      const [result] = await pool.query(
        'UPDATE interests SET status = ? WHERE interest_id = ?',
        [req.body.status, req.params.id]
      );
      if (result.affectedRows === 0) return res.status(404).json({ success: false, message: 'Interest not found' });
      res.json({ success: true, message: 'Interest status updated' });
    } catch (err) {
      console.error('Update interest error:', err);
      res.status(500).json({ success: false, message: 'Failed to update interest' });
    }
  }
);

// Activity history
app.get('/activity', authenticate, async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT
        a.activity_id AS id,
        a.activity_id,
        a.user_id,
        a.property_id,
        a.activity_type,
        a.activity_details,
        a.created_at,
        p.title AS property_title
       FROM activity_history a
       LEFT JOIN properties p ON p.property_id = a.property_id
       WHERE a.user_id = ?
       ORDER BY a.created_at DESC
       LIMIT 100`,
      [req.user.id]
    );
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error('Fetch activity error:', err);
    res.status(500).json({ success: false, message: 'Failed to fetch activity' });
  }
});

app.post('/activity', authenticate,
  body('activity_type').isIn(['VIEWED', 'SAVED', 'UNSAVED', 'INTERESTED', 'EMI_CALCULATED']),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ success: false, errors: errors.array() });

    const { activity_type, property_id, activity_details } = req.body;
    try {
      await pool.query(
        `INSERT INTO activity_history
          (user_id, property_id, activity_type, activity_details)
         VALUES (?, ?, ?, ?)`,
        [req.user.id, property_id || null, activity_type, activity_details || null]
      );
      res.status(201).json({ success: true, message: 'Activity logged' });
    } catch (err) {
      console.error('Log activity error:', err);
      res.status(500).json({ success: false, message: 'Failed to log activity' });
    }
  }
);

app.listen(PORT, () => console.log(`Interaction service running on port ${PORT}`));
