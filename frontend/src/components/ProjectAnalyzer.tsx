import { useState } from 'react';
import { Box, Button, LinearProgress, TextField, Typography } from '@mui/material';
import { analyzeRepository, getAnalysisStatus, AnalysisStatus } from '../api/api';

interface ProjectAnalyzerProps {
  onAnalysisComplete: () => void;
}

export function ProjectAnalyzer({ onAnalysisComplete }: ProjectAnalyzerProps) {
  const [projectId, setProjectId] = useState('');
  const [status, setStatus] = useState<AnalysisStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  const startAnalysis = async () => {
    try {
      setError(null);
      await analyzeRepository(projectId);
      pollStatus();
    } catch (err) {
      setError('Failed to start analysis');
    }
  };

  const pollStatus = async () => {
    try {
      const status = await getAnalysisStatus(projectId);
      setStatus(status);

      if (status.error) {
        setError(status.error);
        return;
      }

      if (status.currentStep !== 'COMPLETED') {
        setTimeout(pollStatus, 1000);
      } else if (status.success) {
        onAnalysisComplete();
      }
    } catch (err) {
      setError('Failed to get analysis status');
    }
  };

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto', textAlign: 'center' }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Chat with Codebase
      </Typography>
      <Typography variant="body1" gutterBottom>
        Enter a GitLab project ID to start analyzing the codebase
      </Typography>
      <Box sx={{ my: 4 }}>
        <TextField
          fullWidth
          label="GitLab Project ID"
          value={projectId}
          onChange={(e) => setProjectId(e.target.value)}
          margin="normal"
        />
        <Button
          variant="contained"
          onClick={startAnalysis}
          disabled={!projectId || status?.currentStep === 'COMPLETED'}
          sx={{ mt: 2 }}
        >
          Analyze Repository
        </Button>
      </Box>
      {status && (
        <Box sx={{ mt: 4 }}>
          <Typography variant="body1" gutterBottom>
            {status.currentStep.replace(/_/g, ' ')}
          </Typography>
          <LinearProgress
            variant="determinate"
            value={status.progress}
            sx={{ mt: 1 }}
          />
        </Box>
      )}
      {error && (
        <Typography color="error" sx={{ mt: 2 }}>
          {error}
        </Typography>
      )}
    </Box>
  );
} 