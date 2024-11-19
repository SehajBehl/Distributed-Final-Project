

import React, { useState } from 'react';
import VersionHistory from './VersionHistory';

const DocumentEditor = () => {
  const [versions, setVersions] = useState([
    { id: '1', versionNumber: 1 },
    { id: '2', versionNumber: 2 },
    { id: '3', versionNumber: 3 },
  ]);
  const [currentVersionId, setCurrentVersionId] = useState('3');

  const handleRollback = (versionId) => {
    console.log(`Rolling back to version ${versionId}`);
    // Simulate rollback by setting the currentVersionId to the selected version ID
    setCurrentVersionId(versionId);

    // Add logic to communicate with the server or state management to update content
  };

  return (
    <div>
      <h2>Document Editor</h2>
      {/* Pass the versions, rollback handler, and current version ID to the VersionHistory component */}
      <VersionHistory
        versions={versions}
        onRollback={handleRollback}
        currentVersionId={currentVersionId}
      />
    </div>
  );
};

export default DocumentEditor;
