


import React from 'react';

const VersionHistory = ({ versions, onRollback, currentVersionId }) => {
  return (
    <div>
      <h3>Version History</h3>
      <ul>
        {versions.map((version) => (
          <li key={version.id}>
            <span>{`Version ${version.versionNumber}`}</span>
            <button
              onClick={() => onRollback(version.id)}
              disabled={version.id === currentVersionId}
            >
              Rollback
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default VersionHistory;
