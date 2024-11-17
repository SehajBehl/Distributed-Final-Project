import React, { useEffect, useState } from 'react';
import axios from 'axios';
import VersionHistory from './VersionHistory';

const DocumentEditor = ({ docId, userId }) => {
  const [content, setContent] = useState('');
  const [versions, setVersions] = useState([]);
  const [currentVersionId, setCurrentVersionId] = useState(null); // To track the current version

  // Fetch document and its version history
  const fetchDocumentVersions = async () => {
    try {
      const response = await axios.get(`http://localhost:5000/api/documents/${docId}/versions`);
      setVersions(response.data);
      if (response.data.length > 0) {
        setContent(response.data[0].content); // Set initial content from the latest version
        setCurrentVersionId(response.data[0].id); // Track the current version ID
      }
    } catch (error) {
      console.log('Error fetching document versions', error);
    }
  };

  useEffect(() => {
    fetchDocumentVersions();
  }, [docId]);

  // Save document content as a new version
  const saveDocument = async () => {
    try {
      await axios.post(`http://localhost:5000/api/documents/${docId}/save`, { content, userId });
      alert('Document saved');
      fetchDocumentVersions(); // Refresh the version list after saving
    } catch (error) {
      console.log('Error saving document', error);
    }
  };

  // Handle rollback to a specific version
  const rollbackDocument = async (versionId) => {
    try {
      await axios.post(`http://localhost:5000/api/documents/${docId}/rollback`, { versionId, userId });
      alert('Document rolled back');
      const response = await axios.get(`http://localhost:5000/api/documents/${docId}/versions`);
      setVersions(response.data);
      const rolledBackVersion = response.data.find(version => version.id === versionId);
      setContent(rolledBackVersion.content); // Set content to the rolled-back version
      setCurrentVersionId(rolledBackVersion.id); // Update current version ID
    } catch (error) {
      console.log('Error rolling back document', error);
    }
  };

  return (
    <div>
      <h2>Document Editor</h2>
      <textarea value={content} onChange={(e) => setContent(e.target.value)} />
      <button onClick={saveDocument}>Save</button>
      <VersionHistory
        docId={docId}
        versions={versions}
        onRollback={rollbackDocument}
        currentVersionId={currentVersionId}
      />
    </div>
  );
};

export default DocumentEditor;
