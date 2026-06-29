export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type AlertStatus =
  | 'UNASSIGNED'
  | 'ASSIGNED'
  | 'ACCEPTED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'ESCALATED';

export interface DocumentMetadata {
  documentId: string;
  originalFilename: string;
  storedPath: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface Alert {
  alertId: string;
  carrierName: string;
  riskScore: number;
  triggeredRules: string;
  severity: Severity;
  assignedDepartment: string;
  status: AlertStatus;
  createdDate: string;
  description?: string;
  documents?: DocumentMetadata[];
  createdBy?: string;
}
