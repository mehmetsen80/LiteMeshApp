import React, { useState, useEffect } from 'react';
import { Dropdown, Button, Modal } from 'react-bootstrap';
import { apiRouteService } from '../../../services/apiRouteService';
import './styles.css';

const VersionControl = ({ routeIdentifier, currentVersion, onVersionChange }) => {
  const [versions, setVersions] = useState([]);
  const [latestVersion, setLatestVersion] = useState(null);
  const [metadata, setMetadata] = useState([]);
  const [showMetadataModal, setShowMetadataModal] = useState(false);
  const [showCompareModal, setShowCompareModal] = useState(false);
  const [compareVersions, setCompareVersions] = useState({ version1: '', version2: '' });
  const [comparison, setComparison] = useState(null);

  useEffect(() => {
    fetchVersions();
    fetchMetadata();
  }, [routeIdentifier]);

  const fetchVersions = async () => {
    try {
      const data = await apiRouteService.getRouteVersions(routeIdentifier);
      const versionNumbers = data.map(version => version.version).sort((a, b) => b - a);
      setVersions(versionNumbers);
      setLatestVersion(versionNumbers[0]);
    } catch (error) {
      console.error('Failed to fetch versions:', error);
    }
  };

  const getFilteredVersions = () => {
    if (!currentVersion) {
      return versions.filter(version => version !== latestVersion);
    }
    return versions.filter(version => version !== currentVersion);
  };

  const fetchMetadata = async () => {
    try {
      const data = await apiRouteService.getVersionMetadata(routeIdentifier);
      setMetadata(data);
    } catch (error) {
      console.error('Failed to fetch metadata:', error);
    }
  };

  const handleVersionSelect = async (version) => {
    try {
      const data = await apiRouteService.getRouteVersion(routeIdentifier, version);
      onVersionChange(data);
    } catch (error) {
      console.error('Failed to fetch version:', error);
    }
  };

  const handleCompare = async () => {
    try {
      const data = await apiRouteService.compareVersions(
        routeIdentifier,
        compareVersions.version1,
        compareVersions.version2
      );
      setComparison(data);
    } catch (error) {
      console.error('Failed to compare versions:', error);
    }
  };

  return (
    <div className="version-control">
      <h2 className="section-title">Version Control</h2>
      <div className="version-control-container">
        <div className="version-actions">
          <Dropdown>
            <Dropdown.Toggle variant="secondary">
              {currentVersion ? `Version ${currentVersion}` : `Latest Version (${latestVersion})`}
            </Dropdown.Toggle>
            <Dropdown.Menu>
              <Dropdown.Item 
                key="latest"
                onClick={() => handleVersionSelect('latest')}
                className={!currentVersion ? 'current-version' : ''}
              >
                Latest Version ({latestVersion}) {!currentVersion && <span className="current-label">(Current)</span>}
              </Dropdown.Item>
              <Dropdown.Divider />
              {getFilteredVersions().map((version) => (
                <Dropdown.Item 
                  key={version} 
                  onClick={() => handleVersionSelect(version)}
                >
                  Version {version}
                </Dropdown.Item>
              ))}
            </Dropdown.Menu>
          </Dropdown>
          <Button 
            variant="outline-info" 
            onClick={() => setShowMetadataModal(true)}
          >
            Version History
          </Button>
          <Button 
            variant="outline-primary" 
            onClick={() => setShowCompareModal(true)}
          >
            Compare Versions
          </Button>
        </div>
      </div>

      {/* Metadata Modal */}
      <Modal show={showMetadataModal} onHide={() => setShowMetadataModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Version History</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="metadata-list">
            {metadata.map((item, index) => (
              <div key={index} className="metadata-item">
                <h5>Version {item.version}</h5>
                <p>Changed by: {item.changedBy}</p>
                <p>Changed at: {new Date(item.timestamp).toLocaleString()}</p>
                <p>Change type: {item.changeType}</p>
                <p>Description: {item.description}</p>
              </div>
            ))}
          </div>
        </Modal.Body>
      </Modal>

      {/* Compare Modal */}
      <Modal show={showCompareModal} onHide={() => setShowCompareModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Compare Versions</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="compare-form">
            <div className="version-selectors">
              <Dropdown>
                <Dropdown.Toggle variant="secondary">
                  Version {compareVersions.version1 || 'Select'}
                </Dropdown.Toggle>
                <Dropdown.Menu>
                  {versions.map((version) => (
                    <Dropdown.Item 
                      key={version} 
                      onClick={() => setCompareVersions({...compareVersions, version1: version})}
                    >
                      Version {version}
                    </Dropdown.Item>
                  ))}
                </Dropdown.Menu>
              </Dropdown>

              <span>vs</span>

              <Dropdown>
                <Dropdown.Toggle variant="secondary">
                  Version {compareVersions.version2 || 'Select'}
                </Dropdown.Toggle>
                <Dropdown.Menu>
                  {versions.map((version) => (
                    <Dropdown.Item 
                      key={version} 
                      onClick={() => setCompareVersions({...compareVersions, version2: version})}
                    >
                      Version {version}
                    </Dropdown.Item>
                  ))}
                </Dropdown.Menu>
              </Dropdown>

              <Button 
                variant="primary" 
                onClick={handleCompare}
                disabled={!compareVersions.version1 || !compareVersions.version2}
              >
                Compare
              </Button>
            </div>

            {comparison && (
              <div className="comparison-result">
                <pre>{JSON.stringify(comparison, null, 2)}</pre>
              </div>
            )}
          </div>
        </Modal.Body>
      </Modal>
    </div>
  );
};

export default VersionControl; 