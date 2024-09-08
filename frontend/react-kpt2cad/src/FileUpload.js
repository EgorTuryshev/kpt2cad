import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  List,
  ListItem,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  IconButton,
  Paper,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { FileIcon, defaultStyles } from 'react-file-icon';

const FileUpload = () => {
  const [files, setFiles] = useState([]);
  const [xslFiles, setXslFiles] = useState([]);
  const [selectedOption, setSelectedOption] = useState('');

  // Загрузка списка файлов XSL с сервера при загрузке компонента
  useEffect(() => {
    fetch('/api/xsl-files')
      .then((response) => response.json())
      .then((data) => setXslFiles(data))
      .catch((error) => console.error('Ошибка при получении файлов XSL:', error));
  }, []);

  const handleRemoveFile = (fileIndex) => {
    setFiles((prevFiles) => prevFiles.filter((_, index) => index !== fileIndex));
  };

  const handleDrop = (event) => {
    event.preventDefault();
    const droppedFiles = Array.from(event.dataTransfer.files);
    handleFiles(droppedFiles);
  };

  const handleDragOver = (event) => {
    event.preventDefault();
  };

  const handleFileChange = (event) => {
    const selectedFiles = Array.from(event.target.files);
    handleFiles(selectedFiles);
  };

  // Обработка файлов для добавления в список и немедленной загрузки
  const handleFiles = (selectedFiles) => {
    const validFiles = selectedFiles.filter((file) =>
      ['xml', 'zip'].includes(file.name.split('.').pop().toLowerCase())
    );

    if (validFiles.length > 0) {
      setFiles((prevFiles) => [...prevFiles, ...validFiles]);
      validFiles.forEach(uploadFile);
    }
  };

  // Функция для загрузки файла на сервер
  const uploadFile = (file) => {
    const formData = new FormData();
    formData.append('files', file);
    formData.append('xslFile', selectedOption); // Добавляем выбранный XSL файл

    fetch('/api/upload', {
      method: 'POST',
      body: formData,
    })
      .then((response) => {
        if (response.ok) {
          console.log(`${file.name} загружен успешно.`);
          return response.blob(); // Получаем файл в виде Blob
        }
        throw new Error('Ошибка загрузки файлов');
      })
      .then((blob) => {
        const url = window.URL.createObjectURL(new Blob([blob]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'processed_result.zip'); // Устанавливаем имя скачиваемого файла
        document.body.appendChild(link);
        link.click();
        link.parentNode.removeChild(link);
      })
      .catch((error) => console.error(`Ошибка при загрузке файла ${file.name}:`, error));
  };

  const handleSelectChange = (event) => {
    setSelectedOption(event.target.value);
  };

  return (
    <Paper elevation={3} sx={{ maxWidth: 500, mx: 'auto', mt: 5, p: 3, borderRadius: 2 }}>
      <Typography variant="h5" gutterBottom align="center">
        Загрузить файлы
      </Typography>

      <Box
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        sx={{
          border: '2px dashed #3f51b5',
          borderRadius: 2,
          p: 4,
          textAlign: 'center',
          cursor: 'pointer',
          mb: 3,
          '&:hover': { backgroundColor: '#f9f9f9' },
        }}
      >
        <input
          type="file"
          multiple
          accept=".xml, .zip"
          onChange={handleFileChange}
          style={{ display: 'none' }}
          id="file-input"
        />
        <label htmlFor="file-input" style={{ cursor: 'pointer' }}>
          <Typography variant="body2" color="textSecondary">
            Перетащите файлы сюда или нажмите, чтобы выбрать
          </Typography>
        </label>
      </Box>

      <Typography variant="subtitle1" gutterBottom>
        Готовы к загрузке
      </Typography>
      <List>
        {files.map((file, index) => (
          <ListItem
            key={index}
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              p: 1,
              mb: 1,
              borderRadius: 1,
              border: '1px solid #ddd',
              boxShadow: 1,
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <Box sx={{ width: 40, height: 40, mr: 2, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <FileIcon
                  extension={file.name.split('.').pop()}
                  {...defaultStyles[file.name.split('.').pop()]}
                  size={24}
                  style={{ width: '100%', height: '100%' }}
                />
              </Box>
              <Box>
                <Typography variant="body2">{file.name}</Typography>
                <Typography variant="caption" color="textSecondary">
                  {file.size} байт
                </Typography>
              </Box>
            </Box>
            <IconButton edge="end" onClick={() => handleRemoveFile(index)}>
              <DeleteIcon />
            </IconButton>
          </ListItem>
        ))}
      </List>

      <FormControl fullWidth sx={{ mb: 2 }}>
        <InputLabel id="select-label">Выберите XSL файл</InputLabel>
        <Select
          labelId="select-label"
          value={selectedOption}
          onChange={handleSelectChange}
        >
          {xslFiles.map((file, index) => (
            <MenuItem key={index} value={file}>
              {file}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </Paper>
  );
};

export default FileUpload;