import { useState } from 'react';
import { Box, Container, CssBaseline, ThemeProvider, createTheme, Tab, Tabs } from '@mui/material';
import { ProjectAnalyzer } from './components/ProjectAnalyzer';
import { Chat } from './components/Chat';
import { DependencyGraph } from './components/DependencyGraph';
import { DependencyData } from './api/api';

const darkTheme = createTheme({
  palette: {
    mode: 'dark',
  },
});

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`tabpanel-${index}`}
      aria-labelledby={`tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

function App() {
  const [isAnalyzed, setIsAnalyzed] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [dependencyData, setDependencyData] = useState<DependencyData>({ nodes: [], links: [] });

  const handleAnalysisComplete = (data: DependencyData) => {
    setDependencyData(data);
    setIsAnalyzed(true);
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  return (
    <ThemeProvider theme={darkTheme}>
      <CssBaseline />
      <Container maxWidth="lg">
        <Box sx={{ my: 4 }}>
          {!isAnalyzed ? (
            <ProjectAnalyzer onAnalysisComplete={handleAnalysisComplete} />
          ) : (
            <Box sx={{ width: '100%' }}>
              <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                <Tabs value={tabValue} onChange={handleTabChange} aria-label="codebase analysis tabs">
                  <Tab label="Chat" />
                  <Tab label="Dependencies" />
                </Tabs>
              </Box>
              <TabPanel value={tabValue} index={0}>
                <Chat />
              </TabPanel>
              <TabPanel value={tabValue} index={1}>
                <DependencyGraph data={dependencyData} />
              </TabPanel>
            </Box>
          )}
        </Box>
      </Container>
    </ThemeProvider>
  );
}

export default App; 