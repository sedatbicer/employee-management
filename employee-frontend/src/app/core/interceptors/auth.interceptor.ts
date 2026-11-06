
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

/**
 * Funktionaler HTTP-Interceptor zur automatischen JWT-Übertragung.
 *
 * Der Interceptor ergänzt ausschließlich Anfragen an die
 * konfigurierten Backend-API-Bereiche um den Authorization-Header.
 *
 * Externe HTTP-Anfragen sowie der Login-Endpunkt werden ausdrücklich
 * von der Token-Übertragung ausgeschlossen.
 *
 * Dadurch wird verhindert, dass Authentifizierungsdaten unbeabsichtigt
 * an andere Dienste übermittelt werden.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  // Ohne vorhandenen Token ist keine Authentifizierung möglich.
  if (!token) {
    return next(req);
  }

  /**
   * Ermittelt den Pfad einer URL.
   *
   * Relative API-Pfade werden gegen die aktuelle Request-URL
   * aufgelöst. Dadurch lassen sich auch absolute und relative
   * HTTP-Anfragen einheitlich behandeln.
   */
  const getRequestPath = (url: string): string => {
    return new URL(url, window.location.origin).pathname;
  };

  /**
   * Prüft, ob eine Anfrage zu einem konfigurierten API-Bereich gehört.
   *
   * Die Pfadgrenze verhindert beispielsweise, dass
   * /api/employees-archive fälschlicherweise als
   * /api/employees erkannt wird.
   */
  const isApiRequest = (requestPath: string, apiPath: string): boolean => {
    return requestPath === apiPath ||
      requestPath.startsWith(`${apiPath}/`);
  };

  const requestPath = getRequestPath(req.url);

  const isEmployeeRequest = isApiRequest(
    requestPath,
    environment.apiEmployee
  );

  const isAuthRequest = isApiRequest(
    requestPath,
    environment.apiAuth
  );

  const isLoginRequest = requestPath === `${environment.apiAuth}/login`;

  // JWT ausschließlich an geschützte Backend-API-Bereiche senden.
  if ((!isEmployeeRequest && !isAuthRequest) || isLoginRequest) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};
