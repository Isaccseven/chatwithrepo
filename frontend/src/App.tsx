import { useState } from 'react';
import { Box, Container, CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { ProjectAnalyzer } from './components/ProjectAnalyzer';
import { Chat } from './components/Chat';

const darkTheme = createTheme({
  palette: {
    mode: 'dark',
  },
});

function App() {
  const [isAnalyzed, setIsAnalyzed] = useState(false);

  return (
    <ThemeProvider theme={darkTheme}>
      <CssBaseline />
      <Container maxWidth="lg">
        <Box sx={{ my: 4 }}>
          {!isAnalyzed ? (
            <ProjectAnalyzer onAnalysisComplete={() => setIsAnalyzed(true)} />
          ) : (
            <Chat />
          )}
        </Box>
      </Container>
    </ThemeProvider>
  );
}

export default App; 