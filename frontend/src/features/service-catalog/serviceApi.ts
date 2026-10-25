import type { Service } from "./service";

export async function getPublishedServices(): Promise<Service[]> {
  const response = await fetch("/api/services");

  if (!response.ok) {
    throw new Error(
      `Không thể tải danh sách dịch vụ. Mã HTTP: ${response.status}`,
    );
  }

  return (await response.json()) as Service[];
}
