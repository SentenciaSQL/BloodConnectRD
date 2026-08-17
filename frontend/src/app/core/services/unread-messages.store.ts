import { Injectable, inject, signal } from '@angular/core';
import { interval } from 'rxjs';

import { ApiService } from './api.service';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class UnreadMessagesStore {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly count = signal(0);
  private started = false;

  start(): void {
    if (this.started) {
      this.refresh();
      return;
    }
    this.started = true;
    this.refresh();
    interval(12000).subscribe(() => this.refresh());
  }

  refresh(): void {
    if (!this.auth.isAuthenticated()) {
      this.count.set(0);
      return;
    }
    this.api.unreadMessageCount().subscribe({
      next: (response) => this.count.set(response.unreadCount ?? 0),
      error: () => undefined,
    });
  }

  set(count: number): void {
    this.count.set(Math.max(0, count));
  }
}
