import { isPlatformBrowser } from '@angular/common';
import { HttpInterceptorFn } from '@angular/common/http';
import { PLATFORM_ID, inject } from '@angular/core';
import { timeout } from 'rxjs';

export const ssrTimeoutInterceptor: HttpInterceptorFn = (request, next) => {
  if (isPlatformBrowser(inject(PLATFORM_ID))) {
    return next(request);
  }
  return next(request).pipe(timeout({ first: 8000 }));
};
