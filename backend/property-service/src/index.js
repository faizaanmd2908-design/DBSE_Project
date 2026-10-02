require('dotenv').config({ path: require('path').resolve(__dirname, '../../../.env') });
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const jwt = require('jsonwebtoken');
const { body, validationResult } = require('express-validator');
const mysql = require('mysql2/promise');

const app = express();
const PORT = Number(process.env.PROPERTY_PORT || 3002);

const GALLERY_IMAGES = [
  'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1000&q=85',
  'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1000&q=85',
  'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=1000&q=85',
  'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=85',
  'https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=1000&q=85',
  'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1000&q=85',
  'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=1000&q=85',
  'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1000&q=85',
  'https://images.unsplash.com/photo-1600047509807-ba8f99d2cdde?w=1000&q=85',
  'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?w=1000&q=85',
  'https://images.unsplash.com/photo-1560448204-603b3fc33ddc?w=1000&q=85',
  'https://images.unsplash.com/photo-1600585154526-990dced4db0d?w=1000&q=85',
  'https://images.unsplash.com/photo-1600573472591-ee6c8e60978f?w=1000&q=85',
  'https://images.unsplash.com/photo-1600047509358-9dc75507daeb?w=1000&q=85',
  'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1000&q=85'
];

async function ensurePropertyGalleries() {
  try {
    const [properties] = await pool.query('SELECT property_id FROM properties ORDER BY property_id');
    for (const property of properties) {
      const [existingRows] = await pool.query(
        'SELECT image_url FROM property_images WHERE property_id = ? ORDER BY is_primary DESC, image_id ASC',
        [property.property_id]
      );
      const existing = new Set(existingRows.map((row) => row.image_url));
      let cursor = (Number(property.property_id) * 3) % GALLERY_IMAGES.length;
      while (existing.size < 3) {
        const imageUrl = GALLERY_IMAGES[cursor % GALLERY_IMAGES.length];
        cursor += 1;
        if (existing.has(imageUrl)) continue;
        await pool.query(
          'INSERT INTO property_images (property_id, image_url, is_primary) VALUES (?, ?, ?)',
          [property.property_id, imageUrl, existingRows.length === 0 ? 1 : 0]
        );
        existing.add(imageUrl);
      }
    }
    console.log('Property gallery check complete (minimum 3 images per property).');
  } catch (err) {
    console.error('Property gallery check failed:', err.message);
  }
}


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
    res.json({ status: 'ok', service: 'property-service', database: 'ok' });
  } catch (err) {
    res.status(503).json({ status: 'degraded', service: 'property-service', database: 'unavailable' });
  }
});

function toOptionalNumber(value, parser) {
  if (value === undefined || value === null || value === '') return null;
  const n = parser(value);
  return Number.isFinite(n) ? n : null;
}

// List properties with filters
app.get('/properties', async (req, res) => {
  try {
    const {
      type,
      minPrice,
      maxPrice,
      bedrooms,
      city,
      location,
      state,
      status,
      search,
      page = 1,
      limit = 50
    } = req.query;

    const safePage = Math.max(1, Number.parseInt(page, 10) || 1);
    const safeLimit = Math.min(100, Math.max(1, Number.parseInt(limit, 10) || 50));

    let sql = `
      SELECT
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
        p.listed_by,
        p.created_at,
        p.updated_at,
        (
          SELECT pi.image_url
          FROM property_images pi
          WHERE pi.property_id = p.property_id AND pi.is_primary = 1
          ORDER BY pi.image_id
          LIMIT 1
        ) AS primary_image,
        (
          SELECT COUNT(*)
          FROM property_images pi
          WHERE pi.property_id = p.property_id
        ) AS image_count
      FROM properties p
      WHERE 1 = 1`;
    const params = [];

    if (status) {
      sql += ' AND p.status = ?';
      params.push(String(status).toUpperCase());
    } else {
      sql += " AND p.status != 'INACTIVE'";
    }

    if (type) {
      sql += ' AND p.property_type = ?';
      params.push(String(type).toUpperCase());
    }

    const min = toOptionalNumber(minPrice, Number.parseFloat);
    const max = toOptionalNumber(maxPrice, Number.parseFloat);
    const beds = toOptionalNumber(bedrooms, Number.parseInt);

    if (min !== null) { sql += ' AND p.price >= ?'; params.push(min); }
    if (max !== null) { sql += ' AND p.price <= ?'; params.push(max); }
    if (beds !== null) { sql += ' AND p.bedrooms >= ?'; params.push(beds); }
    if (city) { sql += ' AND p.city LIKE ?'; params.push(`%${city}%`); }
    if (state) { sql += ' AND p.state LIKE ?'; params.push(`%${state}%`); }
    if (location) { sql += ' AND p.location LIKE ?'; params.push(`%${location}%`); }

    if (search) {
      sql += ' AND (p.title LIKE ? OR p.location LIKE ? OR p.city LIKE ? OR p.description LIKE ?)';
      const q = `%${search}%`;
      params.push(q, q, q, q);
    }

    sql += ' ORDER BY p.created_at DESC LIMIT ? OFFSET ?';
    params.push(safeLimit, (safePage - 1) * safeLimit);

    const [rows] = await pool.query(sql, params);
    res.json({
      success: true,
      data: rows,
      meta: { page: safePage, limit: safeLimit, count: rows.length }
    });
  } catch (err) {
    console.error('List properties error:', err);
    res.status(500).json({ success: false, message: 'Failed to fetch properties' });
  }
});

// Single property with images
app.get('/properties/:id', async (req, res) => {
  try {
    const propertyId = Number.parseInt(req.params.id, 10);
    if (!Number.isInteger(propertyId) || propertyId < 1) {
      return res.status(400).json({ success: false, message: 'Invalid property id' });
    }

    const [properties] = await pool.query(
      `SELECT
        property_id AS id,
        property_id,
        title,
        description,
        property_type,
        location,
        city,
        state,
        pincode,
        price,
        area_sqft,
        bedrooms,
        bathrooms,
        status,
        listed_by,
        created_at,
        updated_at
       FROM properties
       WHERE property_id = ?`,
      [propertyId]
    );

    if (properties.length === 0) {
      return res.status(404).json({ success: false, message: 'Property not found' });
    }

    const [images] = await pool.query(
      `SELECT
        image_id AS id,
        image_url AS url,
        is_primary AS isPrimary
       FROM property_images
       WHERE property_id = ?
       ORDER BY is_primary DESC, image_id ASC`,
      [propertyId]
    );

    res.json({
      success: true,
      data: { ...properties[0], images }
    });
  } catch (err) {
    console.error('Get property error:', err);
    res.status(500).json({ success: false, message: 'Failed to fetch property' });
  }
});

async function ensureSinglePropertyGallery(propertyId) {
  const [rows] = await pool.query(
    'SELECT image_url FROM property_images WHERE property_id = ? ORDER BY is_primary DESC, image_id ASC',
    [propertyId]
  );
  const existing = new Set(rows.map((row) => row.image_url));
  let cursor = (Number(propertyId) * 3) % GALLERY_IMAGES.length;
  while (existing.size < 3) {
    const imageUrl = GALLERY_IMAGES[cursor % GALLERY_IMAGES.length];
    cursor += 1;
    if (existing.has(imageUrl)) continue;
    await pool.query(
      'INSERT INTO property_images (property_id, image_url, is_primary) VALUES (?, ?, 0)',
      [propertyId, imageUrl]
    );
    existing.add(imageUrl);
  }
}

// Create property (admin)
app.post('/properties', authenticate, requireAdmin,
  body('title').trim().isLength({ min: 3 }).withMessage('Title must be at least 3 characters'),
  body('property_type').isIn(['APARTMENT', 'VILLA', 'HOUSE', 'PLOT', 'OTHER']),
  body('price').isFloat({ min: 0.01 }),
  body('area_sqft').isFloat({ min: 0.01 }),
  body('location').notEmpty(),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ success: false, errors: errors.array() });

    const {
      title, description, property_type, location, city, state, pincode,
      price, area_sqft, bedrooms, bathrooms, status = 'AVAILABLE', images
    } = req.body;

    try {
      const [result] = await pool.query(
        `INSERT INTO properties
          (title, description, property_type, location, city, state, pincode,
           price, area_sqft, bedrooms, bathrooms, status, listed_by)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          title,
          description || null,
          property_type,
          location,
          city || 'Hyderabad',
          state || null,
          pincode || null,
          Number(price),
          Number(area_sqft),
          bedrooms === '' || bedrooms === undefined ? null : Number(bedrooms),
          bathrooms === '' || bathrooms === undefined ? null : Number(bathrooms),
          status,
          req.user.id
        ]
      );

      const propertyId = result.insertId;
      if (Array.isArray(images)) {
        for (let i = 0; i < images.length; i++) {
          const imageUrl = String(images[i] || '').trim();
          if (!imageUrl) continue;
          await pool.query(
            'INSERT INTO property_images (property_id, image_url, is_primary) VALUES (?, ?, ?)',
            [propertyId, imageUrl, i === 0]
          );
        }
      }

      await ensureSinglePropertyGallery(propertyId);

      res.status(201).json({
        success: true,
        message: 'Property created',
        data: { id: propertyId }
      });
    } catch (err) {
      console.error('Create property error:', err);
      res.status(500).json({ success: false, message: 'Failed to create property' });
    }
  }
);

// Update property (admin)
app.put('/properties/:id', authenticate, requireAdmin, async (req, res) => {
  try {
    const propertyId = Number.parseInt(req.params.id, 10);
    if (!Number.isInteger(propertyId) || propertyId < 1) {
      return res.status(400).json({ success: false, message: 'Invalid property id' });
    }

    const allowed = [
      'title', 'description', 'property_type', 'location', 'city', 'state',
      'pincode', 'price', 'area_sqft', 'bedrooms', 'bathrooms', 'status'
    ];

    const updates = [];
    const params = [];
    for (const field of allowed) {
      if (req.body[field] !== undefined) {
        updates.push(`${field} = ?`);
        params.push(req.body[field]);
      }
    }

    if (updates.length === 0) {
      return res.status(400).json({ success: false, message: 'No fields to update' });
    }

    params.push(propertyId);
    const [result] = await pool.query(
      `UPDATE properties SET ${updates.join(', ')} WHERE property_id = ?`,
      params
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ success: false, message: 'Property not found' });
    }

    res.json({ success: true, message: 'Property updated' });
  } catch (err) {
    console.error('Update property error:', err);
    res.status(500).json({ success: false, message: 'Failed to update property' });
  }
});

// Update status (admin)
app.patch('/properties/:id/status', authenticate, requireAdmin,
  body('status').isIn(['AVAILABLE', 'RESERVED', 'SOLD', 'INACTIVE']),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ success: false, errors: errors.array() });
    try {
      const [result] = await pool.query(
        'UPDATE properties SET status = ? WHERE property_id = ?',
        [req.body.status, req.params.id]
      );
      if (result.affectedRows === 0) {
        return res.status(404).json({ success: false, message: 'Property not found' });
      }
      res.json({ success: true, message: 'Status updated' });
    } catch (err) {
      console.error('Status update error:', err);
      res.status(500).json({ success: false, message: 'Failed to update status' });
    }
  }
);

// Delete property (admin)
app.delete('/properties/:id', authenticate, requireAdmin, async (req, res) => {
  try {
    const [result] = await pool.query(
      'DELETE FROM properties WHERE property_id = ?',
      [req.params.id]
    );
    if (result.affectedRows === 0) {
      return res.status(404).json({ success: false, message: 'Property not found' });
    }
    res.json({ success: true, message: 'Property deleted' });
  } catch (err) {
    console.error('Delete property error:', err);
    res.status(500).json({ success: false, message: 'Failed to delete property' });
  }
});

// Dashboard statistics (admin)
app.get('/properties/admin/stats', authenticate, requireAdmin, async (req, res) => {
  try {
    const [[{ total }]] = await pool.query('SELECT COUNT(*) AS total FROM properties');
    const [[{ available }]] = await pool.query("SELECT COUNT(*) AS available FROM properties WHERE status = 'AVAILABLE'");
    const [[{ reserved }]] = await pool.query("SELECT COUNT(*) AS reserved FROM properties WHERE status = 'RESERVED'");
    const [[{ sold }]] = await pool.query("SELECT COUNT(*) AS sold FROM properties WHERE status = 'SOLD'");
    const [[{ buyers }]] = await pool.query("SELECT COUNT(*) AS buyers FROM users WHERE role = 'BUYER'");
    const [[{ interests }]] = await pool.query('SELECT COUNT(*) AS interests FROM interests');
    res.json({ success: true, data: { total, available, reserved, sold, buyers, interests } });
  } catch (err) {
    console.error('Stats error:', err);
    res.status(500).json({ success: false, message: 'Failed to fetch stats' });
  }
});

// Add image (admin)
app.post('/properties/:id/images', authenticate, requireAdmin, async (req, res) => {
  const { image_url, is_primary } = req.body;
  if (!image_url) return res.status(400).json({ success: false, message: 'image_url required' });

  try {
    if (is_primary) {
      await pool.query('UPDATE property_images SET is_primary = 0 WHERE property_id = ?', [req.params.id]);
    }
    const [result] = await pool.query(
      'INSERT INTO property_images (property_id, image_url, is_primary) VALUES (?, ?, ?)',
      [req.params.id, image_url, Boolean(is_primary)]
    );
    res.status(201).json({ success: true, data: { id: result.insertId } });
  } catch (err) {
    console.error('Add image error:', err);
    res.status(500).json({ success: false, message: 'Failed to add image' });
  }
});

app.listen(PORT, async () => {
  console.log(`Property service running on port ${PORT}`);
  await ensurePropertyGalleries();
});
