import React, { useState, useEffect } from 'react';
import { v4 as uuidv4 } from 'uuid';
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
  CircularProgress,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { FileIcon, defaultStyles } from 'react-file-icon';

const FileUpload = () => {
  const [files, setFiles] = useState([]);
  const [xslFiles, setXslFiles] = useState([]);
  const [selectedOption, setSelectedOption] = useState('');
  const [sessionId] = useState(uuidv4());
  const [isConverting, setIsConverting] = useState(false);

  useEffect(() => {
    fetch(`/api/xsl-files`)
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Ошибка сервера: ${response.status}`);
        }
        return response.json();
      })
      .then((data) => {
        if (Array.isArray(data)) {
          setXslFiles(data);
          if (data.length > 0) {
            setSelectedOption(data[0]);
          }
        } else {
          console.error('Ожидался массив, но получен другой тип данных:', data);
          setXslFiles([]);
        }
      })
      .catch((error) => console.error('Ошибка при получении файлов XSL:', error));
  }, []);

  const handleRemoveFile = (fileIndex) => {
    const fileToRemove = files[fileIndex];
    // Отправляем запрос на бэкенд для удаления файла
    fetch(`/api/delete`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        fileName: fileToRemove.file.name,
        sessionId: sessionId,
      }),
    })
      .then((response) => {
        if (response.ok) {
          // Удаляем файл из состояния
          setFiles((prevFiles) => prevFiles.filter((_, index) => index !== fileIndex));
        } else {
          throw new Error('Ошибка удаления файла на сервере');
        }
      })
      .catch((error) => {
        console.error(`Ошибка при удалении файла ${fileToRemove.file.name}:`, error);
      });
  };

  // Добавляем функцию handleDrop
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
      const filesWithStatus = validFiles.map((file) => ({
        file,
        status: 'uploading',
      }));
      setFiles((prevFiles) => [...prevFiles, ...filesWithStatus]);
      filesWithStatus.forEach(uploadFile);
    }
  };

  // Функция для загрузки файла на сервер
  const uploadFile = (fileWithStatus) => {
    const { file } = fileWithStatus;
    const formData = new FormData();
    formData.append('file', file);
    formData.append('sessionId', sessionId);
  
    fetch(`/api/upload`, {
      method: 'POST',
      body: formData,
    })
      .then((response) => {
        if (response.ok) {
          console.log(`${file.name} загружен успешно.`);
          updateFileStatus(file, 'uploaded');
        } else {
          throw new Error('Ошибка загрузки файла');
        }
      })
      .catch((error) => {
        console.error(`Ошибка при загрузке файла ${file.name}:`, error);
        updateFileStatus(file, 'error');
      });
  };

  const updateFileStatus = (file, status) => {
    setFiles((prevFiles) =>
      prevFiles.map((f) =>
        f.file === file ? { ...f, status } : f
      )
    );
  };

  const handleSelectChange = (event) => {
    setSelectedOption(event.target.value);
  };

// Обработчик кнопки "Преобразовать файлы"
const handleConvertFiles = () => {
  if (!selectedOption) {
    alert('Пожалуйста, выберите XSL файл перед преобразованием.');
    return;
  }

  setIsConverting(true);

  fetch(`/api/convert`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      xslFile: selectedOption,
      sessionId: sessionId,
    }),
  })
    .then((response) => {
      if (response.ok) {
        const disposition = response.headers.get('Content-Disposition');
        const fileName = disposition
          ? disposition.split('filename=')[1].replace(/"/g, '')
          : 'converted_results.zip';

        return response.blob().then((blob) => ({ blob, fileName }));
      }
      throw new Error('Ошибка преобразования файлов');
    })
    .then(({ blob, fileName }) => {
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      setIsConverting(false);
    })
    .catch((error) => {
      console.error('Ошибка при преобразовании файлов:', error);
      alert(`Произошла ошибка: ${error.message}. Пожалуйста, попробуйте еще раз.`);
      setIsConverting(false);
   });
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
        Файлы
      </Typography>
      <List>
        {files.map((fileObj, index) => {
          const { file, status } = fileObj;
          return (
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
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                {status === 'uploading' && <CircularProgress size={24} sx={{ mr: 2 }} />}
                {status === 'uploaded' && <CheckCircleIcon color="success" sx={{ mr: 2 }} />}
                {status === 'error' && (
                  <Typography variant="caption" color="error" sx={{ mr: 2 }}>
                    Ошибка
                  </Typography>
                )}
                <IconButton edge="end" onClick={() => handleRemoveFile(index)}>
                  <DeleteIcon />
                </IconButton>
              </Box>
            </ListItem>
          );
        })}
      </List>

      <FormControl fullWidth sx={{ mb: 2 }}>
        <InputLabel id="select-label">Выберите XSL файл</InputLabel>
        <Select
          labelId="select-label"
          value={selectedOption}
          onChange={handleSelectChange}
          label="Выберите XSL файл"
        >
          {xslFiles.map((file, index) => (
            <MenuItem key={index} value={file}>
              {file}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <Button
        variant="contained"
        color="primary"
        fullWidth
        onClick={handleConvertFiles}
        disabled={
          files.length === 0 ||
          files.some((f) => f.status !== 'uploaded') ||
          !selectedOption ||
          isConverting
        }
      >
        {isConverting ? <CircularProgress size={24} /> : 'Преобразовать файлы'}
      </Button>
    </Paper>
  );
};

export default FileUpload;