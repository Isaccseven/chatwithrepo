import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

export interface ChatRequest {
  query: string;
}

export interface ChatResponse {
  response: string;
}

export interface AnalysisStatus {
  currentStep: 'NOT_STARTED' | 'FETCHING_FILES' | 'PARSING_AST' | 'STORING_VECTORS' | 'COMPLETED';
  progress: number;
  error: string | null;
  success: boolean;
}

export const analyzeRepository = async (projectId: string): Promise<void> => {
  await api.post(`/analyze/${projectId}`);
};

export const getAnalysisStatus = async (projectId: string): Promise<AnalysisStatus> => {
  const response = await api.get(`/analyze/${projectId}/status`);
  return response.data;
};

export const chat = async (query: string): Promise<string> => {
  const response = await api.post<ChatResponse>('/chat', { query });
  return response.data.response;
}; 