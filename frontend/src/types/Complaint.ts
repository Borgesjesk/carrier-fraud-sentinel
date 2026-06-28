export type ComplaintType =
  | 'PAYMENT'
  | 'INSURANCE'
  | 'ACCIDENT'
  | 'COMMERCIAL_DISPUTE'
  | 'FRAUD'
  | 'REVIEWING';

export interface ComplaintRequest {
  carrierName: string;
  description: string;
  complaintType: ComplaintType;
}
