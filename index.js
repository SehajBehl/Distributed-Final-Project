const express = require ('express');
const bodyParser = require ('body-parser');
const cors = require ('cors');
const { Pool } = require ('pg');

const app = express ();
app.use (cors ());
app.use (bodyParser.json ());

const pool = new Pool ({
    user:'your_db_user',
    host:'localhost',
    database:'your_db_name',
    password:'your_db_password',
    port:5432,
});

app.post ('/api/documents/:docId/save', async (req, res) => {
    const { docId } = req.params;
    const { content, userId } = req.body;
    const query = 'INSERT INTO documents (id, content) VALUES ($1, $2) ON CONFLICT (id) DO UPDATE SET content = $2';
    const values = [docId, content];
    try {
        await pool.query (
            'INSERT INTO document_versions (document_id, content, user_id) VALUES ($1, $2, $3)', [docId, content, userId]);
        res.status (200).send ('Document saved');
    } catch (error) {
        console.log (error);
        res.status (500).send ('Error saving document');
    }
});

app.get ('/api/documents/:docId', async (req, res) => {
    const { docId } = req.params;
    const query = 'SELECT * FROM documents WHERE id = $1';
    const values = [docId];
    try {
        const document_versions = await pool.query (
            'SELECT version_id, content, timestamp, user_id FROM document_versions WHERE document_id = $1 ORDER BY timestamp DESC', [docId]
        );
        res.status (200).j.json(document_versions.rows);
    } catch (error) {
        console.log (error);
        res.status (500).send ('Error getting document');
    }
});

app.post ('/api/documents/:docId/rollback', async (req, res) => {
    const { docId } = req.params;
    const { versionId } = req.body;
    const query = 'SELECT content FROM document_versions WHERE document_id = $1 AND version_id = $2';
    const values = [docId, versionId];
    try {
        const result = await pool.query ('SELECT content FROM document_versions WHERE document_id = $1' [versionId]
    );
        const contentToRollback = result.rows[0].content;
            await pool.query ('INSERT INTO document_versions (document_id, content, user_id) VALUES ($1, $2, $3)', [docId, contentToRollback, userId]);
            res.status (200).send ('Document rolled back to selected version');
          } catch (error) {
        console.log (error);
        res.status (500).send ('Error rolling back document');
    }
})

app.listen (3000, () => {
    console.log ('Server running on port 3000');
})