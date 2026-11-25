export type ServiceStatus = "DRAFT" | "PUBLISHED";

export interface AdminService {
  id: number;
  name: string;
  slug: string;
  shortDescription: string | null;
  description: string | null;
  imageUrl: string | null;
  status: ServiceStatus;
  sortOrder: number;
  priceAmount: number | null;
  priceCurrency: string | null;
  priceUnit: string | null;
}

export interface ServiceUpsertInput {
  name: string;
  slug: string;
  shortDescription: string | null;
  description: string | null;
  imageUrl: string | null;
  sortOrder: number;
  priceAmount: number | null;
  priceCurrency: string;
  priceUnit: string | null;
}
