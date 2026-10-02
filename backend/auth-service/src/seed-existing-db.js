require('dotenv').config({ path: require('path').resolve(__dirname, '../../../.env') });
const bcrypt = require('bcryptjs');
const mysql = require('mysql2/promise');

const properties = [
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
  'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1200&q=88',
  'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1200&q=88',
  'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=1200&q=88',
  'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1200&q=88',
  'https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=1200&q=88',
  'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1200&q=88',
  'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=1200&q=88',
  'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1200&q=88',
  'https://images.unsplash.com/photo-1600047509807-ba8f99d2cdde?w=1200&q=88',
  'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?w=1200&q=88',
  'https://images.unsplash.com/photo-1560448204-603b3fc33ddc?w=1200&q=88',
  'https://images.unsplash.com/photo-1600585154526-990dced4db0d?w=1200&q=88'
];

async function main() {
  const pool = await mysql.createConnection({
    host: process.env.DB_HOST || 'localhost',
    port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'hestia_db'
  });

  const demoEmail = process.env.DEMO_BUYER_EMAIL || 'demo@hestia.com';
  const demoPassword = process.env.DEMO_BUYER_PASSWORD || '123456';
  const adminEmail = process.env.ADMIN_EMAIL || 'admin@hestia.com';
  const adminPassword = process.env.ADMIN_PASSWORD || 'Admin@123456';

  await pool.execute(
    `INSERT INTO users (name,email,phone,password_hash,role)
     VALUES (?,?,?,?,?)
     ON DUPLICATE KEY UPDATE name=VALUES(name), phone=VALUES(phone), password_hash=VALUES(password_hash), role=VALUES(role)`,
    ['Demo Buyer', demoEmail, '9876543210', await bcrypt.hash(demoPassword, 10), 'BUYER']
  );
  await pool.execute(
    `INSERT INTO users (name,email,phone,password_hash,role)
     VALUES (?,?,?,?,?)
     ON DUPLICATE KEY UPDATE name=VALUES(name), phone=VALUES(phone), password_hash=VALUES(password_hash), role=VALUES(role)`,
    ['HESTIA Admin', adminEmail, '9000000001', await bcrypt.hash(adminPassword, 10), 'ADMIN']
  );

  const [[admin]] = await pool.query('SELECT user_id FROM users WHERE email = ?', [adminEmail]);
  if (!admin) throw new Error('Admin seed failed');

  for (let i = 0; i < properties.length; i++) {
    const p = properties[i];
    const [existing] = await pool.query('SELECT property_id FROM properties WHERE title = ? LIMIT 1', [p[0]]);
    let propertyId;
    if (existing.length) {
      propertyId = existing[0].property_id;
      await pool.execute(
        `UPDATE properties SET description=?, property_type=?, location=?, city=?, state=?, pincode=?, price=?, area_sqft=?, bedrooms=?, bathrooms=?, status=?, listed_by=? WHERE property_id=?`,
        [p[1],p[2],p[3],p[4],p[5],p[6],p[7],p[8],p[9],p[10],p[11],admin.user_id,propertyId]
      );
    } else {
      const [r] = await pool.execute(
        `INSERT INTO properties (title,description,property_type,location,city,state,pincode,price,area_sqft,bedrooms,bathrooms,status,listed_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)`,
        [...p, admin.user_id]
      );
      propertyId = r.insertId;
    }

    const [imgCount] = await pool.query('SELECT COUNT(*) AS c FROM property_images WHERE property_id = ?', [propertyId]);
    if (imgCount[0].c === 0) {
      await pool.execute('INSERT INTO property_images (property_id,image_url,is_primary) VALUES (?,?,1)', [propertyId, images[i % images.length]]);
      if (i % 3 === 0) await pool.execute('INSERT INTO property_images (property_id,image_url,is_primary) VALUES (?,?,0)', [propertyId, images[(i+1) % images.length]]);
    }
  }

  console.log(`Ready: ${properties.length} property records ensured.`);
  console.log(`Buyer: ${demoEmail} / ${demoPassword}`);
  console.log(`Admin: ${adminEmail} / ${adminPassword}`);
  await pool.end();
}

main().catch((error) => { console.error(error); process.exit(1); });
