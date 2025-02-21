import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import architectureDiagram from '/images/high_level_architecture.png';
import ImageModal from '../../components/common/ImageModal';
import { useAuth } from '../../contexts/AuthContext';
import './styles.css';

function Home() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [showImageModal, setShowImageModal] = useState(false);

  const handleGetStarted = () => {
    if (isAuthenticated) {
      navigate('/dashboard');
    } else {
      navigate('/login');
    }
  };

  return (
    <div className="home-container">
      <nav className="top-nav">
        <div className="nav-links">
          <a href="#features">Features</a>
          <a href="https://docs.litemesh.org" target="_blank" rel="noopener noreferrer">Docs</a>
          <a href="https://github.com/mehmetsen80/LiteMeshApp" target="_blank" rel="noopener noreferrer">GitHub</a>
          {isAuthenticated ? (
            <Link to="/dashboard" className="auth-link">Dashboard</Link>
          ) : (
            <div className="auth-links">
              <Link to="/login" className="auth-link">Login</Link>
              <Link to="/register" className="auth-link">Register</Link>
            </div>
          )}
        </div>
      </nav>

      <div className="hero-section">
        <div className="hero-content">
          <img 
            src="/images/color_logo.png" 
            alt="LiteMesh Logo" 
            className="hero-logo"
          />
          <div className="hero-separator"></div>
          <a 
            href="https://github.com/mehmetsen80/LiteMeshApp" 
            target="_blank" 
            rel="noopener noreferrer"
            className="version-badge"
          >
            0.9.1v
          </a>
          <div className="hero-separator"></div>
          <h1>Modern API Gateway Solution</h1>
          <p className="hero-text">
            Monitor and manage your microservices with ease
          </p>
          <div className="cta-buttons">
            <button 
              className="cta-button primary"
              onClick={handleGetStarted}
            >
              Get Started
            </button>
            <a 
              href="https://docs.litemesh.org" 
              className="cta-button secondary"
              target="_blank" 
              rel="noopener noreferrer"
            >
              Learn More
            </a>
          </div>
        </div>
      </div>

      <div id="features" className="features-section">
        <h2>Key Features That Power Your APIs</h2>
        <div className="features-grid">
          <div className="feature-card">
            <i className="fas fa-tachometer-alt"></i>
            <h3>High Performance</h3>
            <p>Built for speed and efficiency with minimal overhead, delivering lightning-fast response times and optimal resource utilization</p>
          </div>
          <div className="feature-card">
            <i className="fas fa-shield-alt"></i>
            <h3>Secure by Default</h3>
            <p>Enterprise-grade security with built-in authentication, authorization, and advanced threat protection to safeguard your APIs</p>
          </div>
          <div className="feature-card">
            <i className="fas fa-chart-line"></i>
            <h3>Real-time Metrics</h3>
            <p>Monitor your services with detailed analytics, performance tracking, and customizable dashboards for data-driven decisions</p>
          </div>
        </div>
      </div>

      <div className="benefits-section">
        <div className="container">
          <h2>Why Choose LiteMesh?</h2>
          <div className="benefits-grid">
            <div className="benefit-item">
              <i className="fas fa-rocket"></i>
              <h3>Quick Setup</h3>
              <p>Get started in minutes with our intuitive configuration</p>
            </div>
            <div className="benefit-item">
              <i className="fas fa-code"></i>
              <h3>Developer Friendly</h3>
              <p>Built by developers to make API management intuitive and efficient</p>
            </div>
            <div className="benefit-item">
              <i className="fas fa-layer-group"></i>
              <h3>Scalable Architecture</h3>
              <p>Effortlessly scales from startup to enterprise</p>
            </div>
          </div>
        </div>
      </div>

      <div id="how-it-works" className="architecture-section">
        <h2>How It Works?</h2>
        <div className="architecture-content">
          <div className="architecture-image">
            <img 
              src={architectureDiagram} 
              alt="LiteMesh Architecture" 
              className="arch-diagram clickable"
              onClick={() => setShowImageModal(true)}
            />
          </div>
          <div className="architecture-features">
            <div className="arch-feature">
              <i className="fas fa-shield-alt"></i>
              <div>
                <h3>Security</h3>
                <p>Enterprise-grade authentication, authorization, and API security controls</p>
              </div>
            </div>
            <div className="arch-feature">
              <i className="fas fa-sync-alt"></i>
              <div>
                <h3>Resiliency</h3>
                <p>Advanced circuit breaking, rate limiting, and robust fault tolerance for reliable services</p>
              </div>
            </div>
            <div className="arch-feature">
              <i className="fas fa-route"></i>
              <div>
                <h3>Dynamic Routing</h3>
                <p>Intelligent request routing with automated real-time service discovery</p>
              </div>
            </div>
            <div className="arch-feature">
              <i className="fas fa-chart-bar"></i>
              <div>
                <h3>Analytics</h3>
                <p>Advanced monitoring and real-time performance insights for your services</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <footer className="footer">
        <div className="container">
          <p className="copyright">&copy; 2024 LiteMesh. All rights reserved.</p>
        </div>
      </footer>

      <ImageModal 
        show={showImageModal}
        onHide={() => setShowImageModal(false)}
        imageSrc={architectureDiagram}
      />
    </div>
  );
}

export default Home;