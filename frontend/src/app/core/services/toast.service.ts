import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: number;
  text: string;
  type: 'success' | 'error' | 'info';
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private sequence = 0;
  readonly messages = signal<ToastMessage[]>([]);

  success(text: string): void {
    this.show(text, 'success');
  }

  error(text: string): void {
    this.show(text, 'error');
  }

  info(text: string): void {
    this.show(text, 'info');
  }

  dismiss(id: number): void {
    this.messages.update((messages) => messages.filter((message) => message.id !== id));
  }

  private show(text: string, type: ToastMessage['type']): void {
    const id = ++this.sequence;
    this.messages.update((messages) => [...messages, { id, text, type }]);
    globalThis.setTimeout(() => this.dismiss(id), 4500);
  }
}
