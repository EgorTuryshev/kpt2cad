import React, { useState, useEffect } from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, CircularProgress } from '@mui/material';
import ReactMarkdown from 'react-markdown';

const Changelog = ({ open, onClose }) => {
  const [changelog, setChangelog] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (open) {
      fetch('/api/changelog')
        .then((response) => response.text())
        .then((data) => {
          setChangelog(data);
          setLoading(false);
        })
        .catch((error) => {
          console.error('Ошибка при получении changelog:', error);
          setChangelog('Ошибка загрузки changelog');
          setLoading(false);
        });
    }
  }, [open]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Changelog</DialogTitle>
      <DialogContent dividers>
        {loading ? (
          <CircularProgress />
        ) : (
          <ReactMarkdown className="markdown-content">{changelog}</ReactMarkdown>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          Закрыть
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default Changelog;
