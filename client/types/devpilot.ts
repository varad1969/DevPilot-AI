/**
 * DevPilot RAG Code Intelligence System - TypeScript Interfaces & Types
 * Aligned strictly with Spring Boot Backend DTOs & Domain Models
 */

export type IndexStatus = 'PENDING' | 'INDEXING' | 'READY' | 'FAILED' | 'EXPIRED';

export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM';

export interface User {
  id: string;
  githubId: number;
  githubUsername: string;
  displayName: string;
  avatarUrl: string | null;
}

export interface Repository {
  id: string;
  githubRepoId: number;
  owner: string;
  name: string;
  fullName: string;
  isPrivate: boolean;
  defaultBranch: string;
  language: string | null;
  htmlUrl: string | null;
  description: string | null;
  indexStatus: IndexStatus;
  indexedAt: string | null;
  chunkCount: number;
  filesTotal: number;
  filesProcessed: number;
  errorMessage: string | null;
}

export interface IndexStatusResponse {
  repositoryId: string;
  status: IndexStatus;
  filesTotal: number;
  filesProcessed: number;
  chunkCount: number;
  errorMessage: string | null;
  updatedAt: string | null;
}

export interface Citation {
  filePath: string;
  startLine: number | null;
  endLine: number | null;
  language: string | null;
}

export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  citations: Citation[];
  createdAt: string;
  isStreaming?: boolean;
}

export interface ChatSession {
  id: string;
  repositoryId: string;
  title: string;
  createdAt: string;
}

export interface CreateChatSessionRequest {
  repositoryId: string;
  title?: string;
}

export interface ChatMessageRequest {
  content: string;
}
