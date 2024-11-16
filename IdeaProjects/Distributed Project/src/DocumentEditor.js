import React, { useEffect, useState } from 'react';
import axios from 'axios';
import VersionHistory from './VersionHistory';

const DocumentEditor = ({docId, userId}) => {
  const [content, setContent] = useState('');

  const saveDocument = async () => {
    try {
      await axios.post(`http://localhost:5000/api/documents/${docId}/save`, {content, userId});
      alert('Document saved');
    } catch (error) {
      console.log('Error saving document', error);
    }
  };

  const rollbackDocument = async versionId => {
    try {
      await axios.post(`http://localhost:5000/api/documents/${docId}/rollback`, {versionId, userId});
      alert('Document rolled back');
      const response = await axios.get(`http://localhost:5000/api/documents/${docId/versions}`);
      setContent(response.data[0].content);
    } catch (error) {
      console.log('Error rolling back document', error);
    }
  };

  return (
    <div>
      <h2>Document Editor</h2>
      <textarea value={content} onChange={e => setContent(e.target.value)} />
      <button onClick={saveDocument}>Save</button>
      <VersionHistory docId={docId} onRollback={rollbackDocument} />
    </div>
  );
};

export default DocumentEditor;