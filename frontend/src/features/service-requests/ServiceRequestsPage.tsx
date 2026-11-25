import { useEffect, useState, type FormEvent } from "react";
import type { GameAccount } from "../game-accounts/gameAccount";
import { getGameAccounts } from "../game-accounts/gameAccountApi";
import type { Service } from "../service-catalog/service";
import { getPublishedServices } from "../service-catalog/serviceApi";
import {
  cancelServiceRequest,
  createServiceRequest,
  getServiceRequests,
  updateServiceRequestStatus,
} from "./serviceRequestApi";
import type {
  ServiceRequest,
  ServiceRequestStatus,
} from "./serviceRequest";
import "./ServiceRequestsPage.css";

interface ServiceRequestsPageProps {
  isAdmin: boolean;
}

const ADMIN_TRANSITIONS: Record<
  ServiceRequestStatus,
  ServiceRequestStatus[]
> = {
  NEW: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["DONE", "CANCELLED"],
  DONE: [],
  CANCELLED: [],
};

function getErrorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Đã xảy ra lỗi không xác định.";
}

function formatPrice(
  amount: number,
  currency: string,
  unit: string,
): string {
  const value = new Intl.NumberFormat("vi-VN").format(amount);
  return `${value} ${currency}${unit ? ` / ${unit}` : ""}`;
}

function formatDate(value: string | null): string {
  return value ? value.replace("T", " ") : "—";
}

function statusLabel(status: ServiceRequestStatus): string {
  switch (status) {
    case "NEW":
      return "Mới";
    case "IN_PROGRESS":
      return "Đang thực hiện";
    case "DONE":
      return "Hoàn tất";
    case "CANCELLED":
      return "Đã hủy";
  }
}

export function ServiceRequestsPage({
  isAdmin,
}: ServiceRequestsPageProps) {
  const [requests, setRequests] = useState<ServiceRequest[]>([]);
  const [services, setServices] = useState<Service[]>([]);
  const [accounts, setAccounts] = useState<GameAccount[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const [serviceId, setServiceId] = useState("");
  const [gameAccountId, setGameAccountId] = useState("");
  const [requestedStartAt, setRequestedStartAt] = useState("");
  const [notes, setNotes] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [pendingRequestId, setPendingRequestId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadPage(): Promise<void> {
      setIsLoading(true);
      setLoadError(null);
      try {
        const requestData = await getServiceRequests();
        if (isAdmin) {
          if (!cancelled) {
            setRequests(requestData);
          }
        } else {
          const [serviceData, accountData] = await Promise.all([
            getPublishedServices(),
            getGameAccounts(),
          ]);
          if (!cancelled) {
            setRequests(requestData);
            setServices(serviceData.filter((service) => service.priceAmount !== null));
            setAccounts(accountData.filter((account) => account.status === "ACTIVE"));
          }
        }
      } catch (error: unknown) {
        if (!cancelled) {
          setLoadError(getErrorMessage(error));
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadPage();
    return () => {
      cancelled = true;
    };
  }, [isAdmin, retryKey]);

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    setActionError(null);
    setNotice(null);
    try {
      const created = await createServiceRequest({
        serviceId: Number(serviceId),
        gameAccountId: Number(gameAccountId),
        requestedStartAt: requestedStartAt || null,
        notes: notes.trim() || null,
      });
      setRequests((current) => [...current, created].sort((a, b) => a.id - b.id));
      setNotice("Đã gửi yêu cầu dịch vụ.");
      setServiceId("");
      setGameAccountId("");
      setRequestedStartAt("");
      setNotes("");
    } catch (error: unknown) {
      setActionError(getErrorMessage(error));
    } finally {
      setIsSaving(false);
    }
  }

  async function handleCancel(request: ServiceRequest) {
    setPendingRequestId(request.id);
    setActionError(null);
    setNotice(null);
    try {
      const updated = await cancelServiceRequest(request.id);
      setRequests((current) =>
        current.map((item) => item.id === updated.id ? updated : item),
      );
      setNotice(`Đã hủy yêu cầu #${request.id}.`);
    } catch (error: unknown) {
      setActionError(getErrorMessage(error));
    } finally {
      setPendingRequestId(null);
    }
  }

  async function handleStatusChange(
    request: ServiceRequest,
    status: ServiceRequestStatus,
  ) {
    setPendingRequestId(request.id);
    setActionError(null);
    setNotice(null);
    try {
      const updated = await updateServiceRequestStatus(request.id, status);
      setRequests((current) =>
        current.map((item) => item.id === updated.id ? updated : item),
      );
      setNotice(`Đã cập nhật trạng thái yêu cầu #${request.id}.`);
    } catch (error: unknown) {
      setActionError(getErrorMessage(error));
    } finally {
      setPendingRequestId(null);
    }
  }

  const heading = isAdmin
    ? "Quản lý yêu cầu dịch vụ"
    : "Yêu cầu dịch vụ của tôi";

  return (
    <section className="service-requests" aria-labelledby="service-requests-heading">
      <header className="service-requests__heading">
        <p className="service-requests__eyebrow">
          {isAdmin ? "QUẢN TRỊ" : "DỊCH VỤ"}
        </p>
        <h1 id="service-requests-heading">{heading}</h1>
        <p>
          {isAdmin
            ? "Theo dõi và cập nhật trạng thái các yêu cầu dịch vụ."
            : "Chọn dịch vụ và tài khoản game đang hoạt động để gửi yêu cầu."}
        </p>
      </header>

      {notice && <p className="service-requests__notice" role="status">{notice}</p>}
      {actionError && <p className="service-requests__error" role="alert">{actionError}</p>}

      {isLoading && (
        <div className="service-requests__state" role="status">
          Đang tải yêu cầu dịch vụ...
        </div>
      )}

      {!isLoading && loadError && (
        <div className="service-requests__state service-requests__state--error" role="alert">
          <p>{loadError}</p>
          <button type="button" onClick={() => setRetryKey((current) => current + 1)}>
            Thử tải lại
          </button>
        </div>
      )}

      {!isLoading && !loadError && (
        <>
          {!isAdmin && (
            <form className="service-request-form" onSubmit={handleCreate}>
              <h2>Gửi yêu cầu mới</h2>
              <label>
                Dịch vụ
                <select
                  required
                  value={serviceId}
                  onChange={(event) => setServiceId(event.target.value)}
                  disabled={isSaving || services.length === 0}
                >
                  <option value="">Chọn dịch vụ có giá</option>
                  {services.map((service) => (
                    <option key={service.id} value={service.id}>
                      {service.name} — {formatPrice(
                        service.priceAmount ?? 0,
                        service.priceCurrency ?? "VND",
                        service.priceUnit ?? "",
                      )}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Tài khoản game
                <select
                  required
                  value={gameAccountId}
                  onChange={(event) => setGameAccountId(event.target.value)}
                  disabled={isSaving || accounts.length === 0}
                >
                  <option value="">Chọn tài khoản đang hoạt động</option>
                  {accounts.map((account) => (
                    <option key={account.id} value={account.id}>
                      {account.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Ngày mong muốn (tùy chọn)
                <input
                  type="datetime-local"
                  value={requestedStartAt}
                  onChange={(event) => setRequestedStartAt(event.target.value)}
                  disabled={isSaving}
                />
              </label>
              <label>
                Ghi chú (tùy chọn)
                <textarea
                  value={notes}
                  onChange={(event) => setNotes(event.target.value)}
                  disabled={isSaving}
                />
              </label>
              {services.length === 0 && (
                <p className="service-requests__hint">
                  Hiện chưa có dịch vụ công khai đủ thông tin giá.
                </p>
              )}
              {accounts.length === 0 && (
                <p className="service-requests__hint">
                  Hãy tạo hoặc khôi phục một tài khoản game trước khi gửi yêu cầu.
                </p>
              )}
              <button
                type="submit"
                disabled={isSaving || services.length === 0 || accounts.length === 0}
              >
                {isSaving ? "Đang gửi..." : "Gửi yêu cầu"}
              </button>
            </form>
          )}

          <section className="service-requests__list" aria-labelledby="request-list-heading">
            <h2 id="request-list-heading">
              {isAdmin ? "Tất cả yêu cầu" : "Lịch sử yêu cầu"}
            </h2>
            {requests.length === 0 ? (
              <div className="service-requests__state">
                <p>Chưa có yêu cầu dịch vụ.</p>
              </div>
            ) : (
              <div className="service-requests__table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th scope="col">Yêu cầu</th>
                      {isAdmin && <th scope="col">Customer</th>}
                      <th scope="col">Dịch vụ / giá snapshot</th>
                      <th scope="col">Game Account</th>
                      <th scope="col">Thời hạn</th>
                      <th scope="col">Trạng thái</th>
                      <th scope="col">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {requests.map((request) => (
                      <tr key={request.id}>
                        <td>
                          #{request.id}
                          <small>{formatDate(request.createdAt)}</small>
                          {request.requestedStartAt && (
                            <small>Mong muốn: {formatDate(request.requestedStartAt)}</small>
                          )}
                        </td>
                        {isAdmin && <td>{request.customerId}</td>}
                        <td>
                          <strong>{request.snapshotServiceName}</strong>
                          <small>{formatPrice(
                            request.snapshotPriceAmount,
                            request.snapshotCurrency,
                            request.snapshotPriceUnit,
                          )}</small>
                        </td>
                        <td>
                          {request.gameAccountId}
                          {request.notes && <small>{request.notes}</small>}
                        </td>
                        <td>
                          {formatDate(request.startAt)}
                          <small>đến {formatDate(request.endAt)}</small>
                        </td>
                        <td>
                          <span className={`service-request-status service-request-status--${request.status.toLowerCase()}`}>
                            {statusLabel(request.status)}
                          </span>
                        </td>
                        <td>
                          <div className="service-requests__actions">
                            {!isAdmin && request.status === "NEW" && (
                              <button
                                type="button"
                                disabled={pendingRequestId === request.id}
                                onClick={() => void handleCancel(request)}
                              >
                                {pendingRequestId === request.id ? "Đang hủy..." : "Hủy"}
                              </button>
                            )}
                            {isAdmin && ADMIN_TRANSITIONS[request.status].map((nextStatus) => (
                              <button
                                key={nextStatus}
                                type="button"
                                disabled={pendingRequestId === request.id}
                                onClick={() => void handleStatusChange(request, nextStatus)}
                              >
                                {statusLabel(nextStatus)}
                              </button>
                            ))}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </section>
  );
}
