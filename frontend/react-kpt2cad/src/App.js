import React, { useEffect, useState } from 'react';
import FileUpload from './FileUpload';
import './App.css';
import Changelog from './Changelog';
import { Button } from '@mui/material';

const App = () => {
  const [backendVersion, setBackendVersion] = useState(null);
  const [changelogOpen, setChangelogOpen] = useState(false);
  const frontendVersion = '0.0.3';

  useEffect(() => {
    fetch('/api/version')
      .then((response) => response.json())
      .then((data) => setBackendVersion(data.version))
      .catch((error) => {
        console.error('Ошибка при получении версии backend:', error);
        setBackendVersion('неизвестно');
      });
  }, []);

  const handleOpenChangelog = () => {
    setChangelogOpen(true);
  };

  const handleCloseChangelog = () => {
    setChangelogOpen(false);
  };

  return (
    <div>
      <FileUpload />
      <div className="version-info">
        <p>Frontend: {frontendVersion}</p>
        <p>Backend: {backendVersion || 'Загрузка...'}</p>
        <Button variant="contained" onClick={handleOpenChangelog} size="small" sx={{ mt: 1 }}>
          Посмотреть Changelog
        </Button>
      </div>
      <Changelog open={changelogOpen} onClose={handleCloseChangelog} />
    </div>
  );
};

export default App;
