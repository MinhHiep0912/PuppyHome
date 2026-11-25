export interface Service {
  id: number;
  name: string;
  slug: string;
  shortDescription: string | null;
  description: string | null;
  imageUrl: string | null;
  sortOrder: number;
  priceAmount: number | null;
  priceCurrency: string | null;
  priceUnit: string | null;
}
