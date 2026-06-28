export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type AlertStatus =
  | 'UNASSIGNED'
  | 'ASSIGNED'
  | 'ACCEPTED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'ESCALATED';

export interface Alert {
  alertId: string;
  carrierName: string;
  riskScore: number;
  triggeredRules: string;
  severity: Severity;
  assignedDepartment: string;
  status: AlertStatus;
  createdDate: string;
}
