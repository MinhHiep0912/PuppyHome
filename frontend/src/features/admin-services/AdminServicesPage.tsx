import { useEffect, useState, type FormEvent } from "react";
import {
  AdminServiceApiError,
  createAdminService,
  getAdminServices,
  softDeleteAdminService,
  updateAdminService,
  updateAdminServiceStatus,
} from "./adminServiceApi";
import type {
  AdminService,
  ServiceStatus,
  ServiceUpsertInput,
} from "./adminService";
import "./AdminServicesPage.css";

interface ServiceFormValues {
  name: string;
  slug: string;
  shortDescription: string;
  description: string;
  imageUrl: string;
  sortOrder: string;
  priceAmount: string;
  priceCurrency: string;
  priceUnit: string;
}

const EMPTY_FORM: ServiceFormValues = {
  name: "",
  slug: "",
  shortDescription: "",
  description: "",
  imageUrl: "",
  sortOrder: "0",
  priceAmount: "",
  priceCurrency: "VND",
  priceUnit: "",
};

function getErrorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Đã xảy ra lỗi không xác định.";
}

function toUpsertInput(values: ServiceFormValues): ServiceUpsertInput {
  return {
    name: values.name.trim(),
    slug: values.slug.trim(),
    shortDescription: values.shortDescription.trim() || null,
    description: values.description.trim() || null,
    imageUrl: values.imageUrl.trim() || null,
    sortOrder: Number(values.sortOrder),
    priceAmount: values.priceAmount.trim() === ""
      ? null
      : Number(values.priceAmount),
    priceCurrency: values.priceCurrency.trim().toUpperCase() || "VND",
    priceUnit: values.priceUnit.trim() || null,
  };
}

function orderServices(services: AdminService[]): AdminService[] {
  return [...services].sort(
    (left, right) =>
      left.sortOrder - right.sortOrder || left.id - right.id,
  );
}

function isPermissionError(error: unknown): boolean {
  return (
    error instanceof AdminServiceApiError &&
    (error.status === 401 || error.status === 403)
  );
}

export function AdminServicesPage() {
  const [services, setServices] = useState<AdminService[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const [formValues, setFormValues] =
    useState<ServiceFormValues>(EMPTY_FORM);
  const [editingServiceId, setEditingServiceId] = useState<number | null>(
    null,
  );
  const [isSaving, setIsSaving] = useState(false);
  const [pendingServiceId, setPendingServiceId] = useState<number | null>(
    null,
  );

  useEffect(() => {
    let cancelled = false;

    async function loadServices(): Promise<void> {
      setIsLoading(true);
      setLoadError(null);
      setPermissionDenied(false);

      try {
        const data = await getAdminServices();
        if (!cancelled) {
          setServices(orderServices(data));
        }
      } catch (error: unknown) {
        if (!cancelled) {
          if (isPermissionError(error)) {
            setPermissionDenied(true);
          } else {
            setLoadError(getErrorMessage(error));
          }
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadServices();
    return () => {
      cancelled = true;
    };
  }, [retryKey]);

  function startEditing(service: AdminService): void {
    setEditingServiceId(service.id);
    setFormValues({
      name: service.name,
      slug: service.slug,
      shortDescription: service.shortDescription ?? "",
      description: service.description ?? "",
      imageUrl: service.imageUrl ?? "",
      sortOrder: String(service.sortOrder),
      priceAmount: service.priceAmount === null
        ? ""
        : String(service.priceAmount),
      priceCurrency: service.priceCurrency ?? "VND",
      priceUnit: service.priceUnit ?? "",
    });
    setFeedback(null);
    setActionError(null);
  }

  function resetForm(): void {
    setEditingServiceId(null);
    setFormValues(EMPTY_FORM);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    setFeedback(null);
    setActionError(null);

    try {
      const input = toUpsertInput(formValues);
      if (editingServiceId === null) {
        const created = await createAdminService(input);
        setServices((current) => orderServices([...current, created]));
        setFeedback(`Đã tạo dịch vụ “${created.name}” ở trạng thái DRAFT.`);
      } else {
        const updated = await updateAdminService(editingServiceId, input);
        setServices((current) =>
          orderServices(
            current.map((service) =>
              service.id === updated.id ? updated : service,
            ),
          ),
        );
        setFeedback(`Đã cập nhật dịch vụ “${updated.name}”.`);
      }
      resetForm();
    } catch (error: unknown) {
      if (isPermissionError(error)) {
        setPermissionDenied(true);
      } else {
        setActionError(getErrorMessage(error));
      }
    } finally {
      setIsSaving(false);
    }
  }

  async function handleStatusChange(service: AdminService): Promise<void> {
    const nextStatus: ServiceStatus =
      service.status === "PUBLISHED" ? "DRAFT" : "PUBLISHED";
    setPendingServiceId(service.id);
    setFeedback(null);
    setActionError(null);

    try {
      const updated = await updateAdminServiceStatus(service.id, nextStatus);
      setServices((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
      setFeedback(
        `Đã chuyển “${updated.name}” sang ${updated.status}. Thay đổi sẽ hiển thị trong danh mục công khai sau khi tải lại.`,
      );
    } catch (error: unknown) {
      if (isPermissionError(error)) {
        setPermissionDenied(true);
      } else {
        setActionError(getErrorMessage(error));
      }
    } finally {
      setPendingServiceId(null);
    }
  }

  async function handleSoftDelete(service: AdminService): Promise<void> {
    const confirmed = window.confirm(
      `Đưa “${service.name}” về DRAFT? Dịch vụ sẽ không còn hiển thị công khai; bản ghi không bị xóa vĩnh viễn.`,
    );
    if (!confirmed) {
      return;
    }

    setPendingServiceId(service.id);
    setFeedback(null);
    setActionError(null);

    try {
      await softDeleteAdminService(service.id);
      setServices((current) =>
        current.map((item) =>
          item.id === service.id ? { ...item, status: "DRAFT" } : item,
        ),
      );
      setFeedback(`Đã đưa “${service.name}” về DRAFT; bản ghi được giữ lại.`);
    } catch (error: unknown) {
      if (isPermissionError(error)) {
        setPermissionDenied(true);
      } else {
        setActionError(getErrorMessage(error));
      }
    } finally {
      setPendingServiceId(null);
    }
  }

  if (permissionDenied) {
    return (
      <section className="admin-services state-panel state-panel--error">
        <h1>Không có quyền truy cập</h1>
        <p>Phiên đăng nhập không hợp lệ hoặc tài khoản không có quyền Admin.</p>
      </section>
    );
  }

  return (
    <section className="admin-services" aria-labelledby="admin-services-heading">
      <div className="admin-services__heading">
        <div>
          <p className="eyebrow">QUẢN TRỊ</p>
          <h1 id="admin-services-heading">Quản lý dịch vụ</h1>
          <p>
            Tạo, chỉnh sửa và quản lý trạng thái hiển thị dịch vụ. DELETE chỉ
            đưa dịch vụ về DRAFT, không xóa vĩnh viễn.
          </p>
        </div>
        {!isLoading && !loadError && (
          <span className="service-count">{services.length} dịch vụ</span>
        )}
      </div>

      {isLoading && (
        <div className="state-panel" role="status">
          <span className="spinner" aria-hidden="true" />
          <p>Đang tải danh sách dịch vụ quản trị...</p>
        </div>
      )}

      {!isLoading && loadError && (
        <div className="state-panel state-panel--error" role="alert">
          <h2>Chưa thể tải danh sách dịch vụ</h2>
          <p>{loadError}</p>
          <button
            className="retry-button"
            type="button"
            onClick={() => setRetryKey((current) => current + 1)}
          >
            Thử tải lại
          </button>
        </div>
      )}

      {!isLoading && !loadError && (
        <>
          {feedback && (
            <p className="admin-services__feedback" role="status">
              {feedback}
            </p>
          )}
          {actionError && (
            <p className="admin-services__error" role="alert">
              {actionError}
            </p>
          )}

          <section
            className="admin-services__table-panel"
            aria-labelledby="service-list-heading"
          >
            <h2 id="service-list-heading">Danh sách dịch vụ</h2>
            {services.length === 0 ? (
              <div className="state-panel">
                <h3>Chưa có dịch vụ</h3>
                <p>Tạo dịch vụ đầu tiên bằng biểu mẫu bên dưới.</p>
              </div>
            ) : (
              <div className="admin-services__table-wrap">
                <table className="admin-services__table">
                  <thead>
                    <tr>
                      <th scope="col">Dịch vụ</th>
                      <th scope="col">Thứ tự</th>
                      <th scope="col">Giá</th>
                      <th scope="col">Trạng thái</th>
                      <th scope="col">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {services.map((service) => (
                      <tr key={service.id}>
                        <td>
                          <strong>{service.name}</strong>
                          <span className="admin-services__slug">
                            {service.slug}
                          </span>
                        </td>
                        <td>{service.sortOrder}</td>
                        <td>
                          {service.priceAmount === null
                            ? "Chưa đặt giá"
                            : `${new Intl.NumberFormat("vi-VN").format(service.priceAmount)} ${service.priceCurrency ?? "VND"}${service.priceUnit ? ` / ${service.priceUnit}` : ""}`}
                        </td>
                        <td>
                          <span
                            className={`service-status service-status--${service.status.toLowerCase()}`}
                          >
                            {service.status}
                          </span>
                        </td>
                        <td>
                          <div className="admin-services__row-actions">
                            <button
                              type="button"
                              onClick={() => startEditing(service)}
                              disabled={pendingServiceId !== null || isSaving}
                            >
                              Sửa
                            </button>
                            <button
                              type="button"
                              onClick={() => void handleStatusChange(service)}
                              disabled={pendingServiceId !== null || isSaving}
                            >
                              {service.status === "PUBLISHED"
                                ? "Chuyển về DRAFT"
                                : "Xuất bản"}
                            </button>
                            <button
                              className="admin-services__delete"
                              type="button"
                              onClick={() => void handleSoftDelete(service)}
                              disabled={pendingServiceId !== null || isSaving}
                            >
                              Đưa về DRAFT
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section
            className="admin-services__form-panel"
            aria-labelledby="service-form-heading"
          >
            <h2 id="service-form-heading">
              {editingServiceId === null
                ? "Tạo dịch vụ"
                : "Chỉnh sửa dịch vụ"}
            </h2>
            <form className="admin-service-form" onSubmit={handleSubmit}>
              <label>
                Tên dịch vụ
                <input
                  name="name"
                  value={formValues.name}
                  onChange={(event) =>
                    setFormValues((current) => ({
                      ...current,
                      name: event.target.value,
                    }))
                  }
                  required
                  maxLength={150}
                  disabled={isSaving}
                />
              </label>
              <label>
                Slug
                <input
                  name="slug"
                  value={formValues.slug}
                  onChange={(event) =>
                    setFormValues((current) => ({
                      ...current,
                      slug: event.target.value,
                    }))
                  }
                  required
                  maxLength={180}
                  disabled={isSaving}
                />
              </label>
              <label>
                Thứ tự hiển thị
                <input
                  name="sortOrder"
                  type="number"
                  step="1"
                  value={formValues.sortOrder}
                  onChange={(event) =>
                    setFormValues((current) => ({
                      ...current,
                      sortOrder: event.target.value,
                    }))
                  }
                  required
                  disabled={isSaving}
                />
              </label>
              <label>
                Giá (để trống nếu chưa có)
                <input
                  name="priceAmount"
                  type="number"
                  min="0"
                  step="1"
                  value={formValues.priceAmount}
                  onChange={(event) =>
                    setFormValues((current) => ({
                      ...current,
                      priceAmount: event.target.value,
                    }))
                  }
                  disabled={isSaving}
                />
              </label>
              <label>
                Đơn vị tiền tệ
                <input
                  name="priceCurrency"
                  value={formValues.priceCurrency}
                  maxLength={3}
                  onChange={(event) =>
                    setFormValues((current) => ({
                      ...current,
                      priceCurrency: event.target.value,
                    }))
                  }
                  required={formValues.priceAmount.trim() !== ""}
                  disabled={isSaving}
                />
              </label>
              <label>
                Đơn vị dịch vụ (tùy chọn)
                <input
                  name="priceUnit"
                  value={formValues.priceUnit}
                  maxLength={32}
                  onChange={(event) =>
                    setFormValues((current) => ({
                      ...current,
                      priceUnit: event.target.value,
                    }))
                  }
                  disabled={isSaving}
                />
              </label>
              <label>
                Mô tả ngắn
                <textarea
                  name="shortDescription"
                  value={formValues.shortDescription}
                  onChange={(event) =>
                    setFormValues((current) => ({
                      ...current,
                      shortDescription: event.target.value,
                    }))
                  }
                  maxLength={500}
                  disabled={isSaving}
                />
              </label>
              <label>
                Mô tả
                <textarea
                  name="description"
                  value={formValues.description}
                  onChange={(event) =>
                    setFormValues((current) => ({
                      ...current,
                      description: event.target.value,
                    }))
                  }
                  disabled={isSaving}
                />
              </label>
              <label>
                URL hình ảnh
                <input
                  name="imageUrl"
                  type="text"
                  value={formValues.imageUrl}
                  onChange={(event) =>
                    setFormValues((current) => ({
                      ...current,
                      imageUrl: event.target.value,
                    }))
                  }
                  maxLength={2048}
                  disabled={isSaving}
                />
              </label>
              <div className="admin-service-form__actions">
                <button type="submit" disabled={isSaving}>
                  {isSaving
                    ? "Đang lưu..."
                    : editingServiceId === null
                      ? "Tạo dịch vụ"
                      : "Lưu thay đổi"}
                </button>
                {editingServiceId !== null && (
                  <button
                    className="admin-service-form__cancel"
                    type="button"
                    onClick={resetForm}
                    disabled={isSaving}
                  >
                    Hủy chỉnh sửa
                  </button>
                )}
              </div>
            </form>
          </section>
        </>
      )}
    </section>
  );
}
