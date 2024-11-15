import React, { useEffect, useState } from 'react';
import axios from 'axios';

const VersionHistory = ({docId, onRollback}) => {
  const [versions, setVersions] = useState([]);

  useEffect(() => {
    const fetchVersions = async () => {
        try {
            const response = await axios.get(`http://localhost:5000/api/documents/${docId}/versions`);
            setVersions(response.data);
        }
        catch (error) {
            console.log('Error fetching versions', error);
        }
    };

            fetchVersions();
          }, [docId]);

    return (
        <div>
            <h2>Version History</h2>
            <ul>
                {versions.map(version => (
                    <li key={version.version_id}>
                        <p>{version.timestamp}</p>
                        <span>new Date(version.tmestamp).toLocaleString()</span>
                        <button onClick={() => onRollback(version.version_id)}>Rollback</button>
                    </li>
                ))}
            </ul>
        </div>
    );

};

export default VersionHistory;