require('dotenv').config({ path: require('path').resolve(__dirname, '../../../.env') });
const bcrypt = require('bcryptjs');
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

async function seed() {
  const pool = await mysql.createConnection({
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '3306'),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    multipleStatements: true
  });

  console.log('Applying schema...');
  const schema = fs.readFileSync(path.join(__dirname, '../../../database/00_full_schema.sql'), 'utf8');
  await pool.query(schema);

  await pool.query('USE hestia_db');
  await pool.query('SET FOREIGN_KEY_CHECKS=0');
  for (const t of ['activity_history','interests','favourites','property_images','properties','users']) {
    await pool.query(`TRUNCATE TABLE ${t}`);
  }
  await pool.query('SET FOREIGN_KEY_CHECKS=1');

  const demoPass = process.env.DEMO_BUYER_PASSWORD || '123456';
  const adminPass = process.env.ADMIN_PASSWORD || 'Admin@123456';
  const demoHash = await bcrypt.hash(demoPass, 10);
  const adminHash = await bcrypt.hash(adminPass, 10);
  const otherHash = await bcrypt.hash('123456', 10);

  await pool.query(
    `INSERT INTO users (name, email, phone, password_hash, role) VALUES
     (?, ?, '9000000001', ?, 'ADMIN'),
     (?, ?, '9876543210', ?, 'BUYER'),
     ('Priya Sharma', 'priya@example.com', '9123456789', ?, 'BUYER'),
     ('Rahul Verma', 'rahul@example.com', '9988776655', ?, 'BUYER')`,
    [
      'Admin User', process.env.ADMIN_EMAIL || 'admin@hestia.com', adminHash,
      'Demo Buyer', process.env.DEMO_BUYER_EMAIL || 'demo@hestia.com', demoHash,
      otherHash, otherHash
    ]
  );
  console.log('Users seeded. Demo:', demoPass, '| Admin:', adminPass);

  const props = [
    ['Modern 3BHK in Kondapur','Spacious 3BHK with modular kitchen and clubhouse. Close to IT hubs.','APARTMENT','Kondapur','Hyderabad','Telangana','500084',12500000,1650,3,3,'AVAILABLE'],
    ['Elegant Villa Gachibowli','Premium villa with private garden and smart home features.','VILLA','Gachibowli','Hyderabad','Telangana','500032',45000000,4200,4,5,'AVAILABLE'],
    ['2BHK near Hitech City','Well-ventilated 2BHK with skyline balcony. Metro nearby.','APARTMENT','Hitech City','Hyderabad','Telangana','500081',8500000,1150,2,2,'AVAILABLE'],
    ['Independent House Manikonda','Courtyard house with good ORR connectivity.','HOUSE','Manikonda','Hyderabad','Telangana','500089',18500000,2800,3,3,'AVAILABLE'],
    ['HMDA Plot Kokapet','Clear title plot ready for construction near metro corridor.','PLOT','Kokapet','Hyderabad','Telangana','500075',9800000,2400,null,null,'AVAILABLE'],
    ['Skyline Apartment Madhapur','High-rise 3BHK with infinity pool and concierge.','APARTMENT','Madhapur','Hyderabad','Telangana','500081',15800000,1850,3,3,'AVAILABLE'],
    ['Garden Villa Nanakramguda','East-facing villa with landscaped garden.','VILLA','Nanakramguda','Hyderabad','Telangana','500008',38000000,3800,4,4,'AVAILABLE'],
    ['Affordable 2BHK Miyapur','Gated community with parks near Miyapur Metro.','APARTMENT','Miyapur','Hyderabad','Telangana','500049',6200000,980,2,2,'AVAILABLE'],
    ['Luxury Penthouse Gachibowli','Penthouse with terrace garden and private elevator.','APARTMENT','Gachibowli','Hyderabad','Telangana','500032',32000000,3100,4,4,'RESERVED'],
    ['Family House Kompally','Near schools and hospitals, spacious independent house.','HOUSE','Kompally','Hyderabad','Telangana','500100',14200000,2400,3,3,'AVAILABLE'],
    ['Corner Plot Narsingi','Dual road access, clear title near ORR.','PLOT','Narsingi','Hyderabad','Telangana','500075',12500000,3000,null,null,'AVAILABLE'],
    ['Smart Home Kondapur','Automated 3BHK with IoT and energy efficient design.','APARTMENT','Kondapur','Hyderabad','Telangana','500084',14500000,1720,3,3,'AVAILABLE'],
    ['Lake View Villa Madhapur','Villa overlooking lake with private jetty.','VILLA','Madhapur','Hyderabad','Telangana','500081',52000000,4800,5,5,'AVAILABLE'],
    ['Compact 1BHK Hitech City','Furnished, ideal for professionals near Mindspace.','APARTMENT','Hitech City','Hyderabad','Telangana','500081',4800000,650,1,1,'AVAILABLE'],
    ['Duplex House Manikonda','Open kitchen, double-height living room.','HOUSE','Manikonda','Hyderabad','Telangana','500089',21500000,3200,4,4,'AVAILABLE'],
    ['Investment Plot Kokapet','High appreciation near Financial District.','PLOT','Kokapet','Hyderabad','Telangana','500075',7500000,1800,null,null,'AVAILABLE'],
    ['Premium 4BHK Gachibowli','Servant room, study, multiple balconies.','APARTMENT','Gachibowli','Hyderabad','Telangana','500032',22500000,2650,4,4,'AVAILABLE'],
    ['Serene Villa Miyapur','Quiet gated community with 24x7 security.','VILLA','Miyapur','Hyderabad','Telangana','500049',29500000,3500,4,4,'SOLD'],
    ['Modern 3BHK Nanakramguda','Smart locks and energy monitoring near TCS.','APARTMENT','Nanakramguda','Hyderabad','Telangana','500008',13200000,1580,3,3,'AVAILABLE'],
    ['Family Home Kompally','Large backyard with fruit trees.','HOUSE','Kompally','Hyderabad','Telangana','500100',16800000,2600,3,3,'AVAILABLE'],
    ['Elite Villa Kokapet','Infinity pool overlooking hills.','VILLA','Kokapet','Hyderabad','Telangana','500075',68000000,5500,5,6,'AVAILABLE'],
    ['City View 2BHK Madhapur','Floor-to-ceiling windows, skyline views.','APARTMENT','Madhapur','Hyderabad','Telangana','500081',9800000,1200,2,2,'AVAILABLE'],
    ['Banjara Hills Apartment','Premium 3BHK in prestigious locality.','APARTMENT','Banjara Hills','Hyderabad','Telangana','500034',28000000,2100,3,3,'AVAILABLE'],
    ['Jubilee Hills Villa','Luxury villa with home theatre.','VILLA','Jubilee Hills','Hyderabad','Telangana','500033',75000000,6000,5,6,'AVAILABLE']
  ];

  const images = [
    'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800&q=80',
    'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=800&q=80',
    'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&q=80',
    'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800&q=80',
    'https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800&q=80',
    'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800&q=80',
    'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=800&q=80',
    'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80',
    'https://images.unsplash.com/photo-1600047509807-ba8f99d2cdde?w=800&q=80',
    'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?w=800&q=80',
    'https://images.unsplash.com/photo-1560448204-603b3fc33ddc?w=800&q=80',
    'https://images.unsplash.com/photo-1600585154526-990dced4db0d?w=800&q=80',
    'https://images.unsplash.com/photo-1600573472591-ee6c8e60978f?w=800&q=80',
    'https://images.unsplash.com/photo-1600047509358-9dc75507daeb?w=800&q=80',
    'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=800&q=80'
  ];

  for (let i = 0; i < props.length; i++) {
    const p = props[i];
    const [r] = await pool.query(
      `INSERT INTO properties (title, description, property_type, location, city, state, pincode, price, area_sqft, bedrooms, bathrooms, status, listed_by)
       VALUES (?,?,?,?,?,?,?,?,?,?,?,?,1)`,
      p
    );
    const pid = r.insertId;
    for (let imageIndex = 0; imageIndex < 3; imageIndex++) {
      await pool.query(
        `INSERT INTO property_images (property_id, image_url, is_primary) VALUES (?, ?, ?)`,
        [pid, images[(i * 3 + imageIndex) % images.length], imageIndex === 0 ? 1 : 0]
      );
    }
  }
  console.log(`Seeded ${props.length} properties with images.`);
  await pool.end();
  console.log('Seed complete.');
}

seed().catch(e => { console.error(e); process.exit(1); });
