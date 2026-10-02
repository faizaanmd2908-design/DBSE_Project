require('dotenv').config({ path: require('path').resolve(__dirname, '../../../.env') });
const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');

const app = express();
const PORT = process.env.GATEWAY_PORT || 3000;

const AUTH_URL = process.env.AUTH_SERVICE_URL || 'http://localhost:3001';
const PROPERTY_URL = process.env.PROPERTY_SERVICE_URL || 'http://localhost:3002';
const INTERACTION_URL = process.env.INTERACTION_SERVICE_URL || 'http://localhost:3003';

app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors({ origin: process.env.CORS_ORIGIN || '*' }));
app.use(morgan('dev'));

app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'api-gateway',
    upstreams: { auth: AUTH_URL, property: PROPERTY_URL, interaction: INTERACTION_URL }
  });
});

// Auth routes
app.use('/auth', createProxyMiddleware({
  target: AUTH_URL,
  changeOrigin: true,
  pathRewrite: { '^/': '/auth/' },
  onError: (err, req, res) => {
    console.error('Auth proxy error:', err.message);
    res.status(503).json({ success: false, message: 'Auth service unavailable' });
  }
}));

// Property routes
app.use('/properties', createProxyMiddleware({
  target: PROPERTY_URL,
  changeOrigin: true,
  pathRewrite: { '^/': '/properties/' },
  onError: (err, req, res) => {
    console.error('Property proxy error:', err.message);
    res.status(503).json({ success: false, message: 'Property service unavailable' });
  }
}));

// Interaction routes
app.use('/favourites', createProxyMiddleware({
  target: INTERACTION_URL,
  changeOrigin: true,
  pathRewrite: { '^/': '/favourites/' },
  onError: (err, req, res) => {
    console.error('Interaction proxy error:', err.message);
    res.status(503).json({ success: false, message: 'Interaction service unavailable' });
  }
}));

app.use('/interests', createProxyMiddleware({
  target: INTERACTION_URL,
  changeOrigin: true,
  pathRewrite: { '^/': '/interests/' },
  onError: (err, req, res) => {
    res.status(503).json({ success: false, message: 'Interaction service unavailable' });
  }
}));

app.use('/activity', createProxyMiddleware({
  target: INTERACTION_URL,
  changeOrigin: true,
  pathRewrite: { '^/': '/activity/' },
  onError: (err, req, res) => {
    res.status(503).json({ success: false, message: 'Interaction service unavailable' });
  }
}));

app.listen(PORT, () => {
  console.log(`API Gateway running on port ${PORT}`);
  console.log(`  Auth      -> ${AUTH_URL}`);
  console.log(`  Property  -> ${PROPERTY_URL}`);
  console.log(`  Interact  -> ${INTERACTION_URL}`);
});
