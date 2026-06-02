import { ApplicationConfig, Component, inject } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter, Router, RouterLink, RouterLinkActive, RouterOutlet, Routes } from '@angular/router';
import { HttpClient, provideHttpClient, withInterceptors, HttpInterceptorFn } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import Hls from 'hls.js';

const API = '/api';

interface User { id: number; name: string; email: string; role: 'ADMIN' | 'USER'; active: boolean; }
interface AuthResponse { accessToken: string; refreshToken: string; user: User; }
type ChannelTestStatus = 'ONLINE' | 'OFFLINE' | 'INVALID' | 'UNKNOWN';
interface IptvList { id: number; name: string; description?: string; sourceType: 'FILE' | 'URL'; sourceUrl?: string; originalFileName?: string; status: string; totalChannels: number; errorMessage?: string; lastImportAt?: string; }
interface Channel { id: number; name: string; streamUrl: string; groupTitle: string; logoUrl?: string; favorite: boolean; active: boolean; testStatus?: ChannelTestStatus; testHttpStatus?: number; testMessage?: string; lastTestAt?: string; iptvListId: number; iptvListName: string; }
interface ChannelTestResult { channelId: number; status: ChannelTestStatus; httpStatus?: number; message: string; }
interface ChannelTestBatch { batchId: string; totalChannels: number; status: string; message: string; }
interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
interface Dashboard { totalLists: number; totalChannels: number; totalGroups: number; totalFavorites: number; recentLists: IptvList[]; recentChannels: Channel[]; }

class AuthStore {
  get token() { return localStorage.getItem('accessToken'); }
  get user(): User | null { const raw = localStorage.getItem('user'); return raw ? JSON.parse(raw) as User : null; }
  save(response: AuthResponse) {
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('user', JSON.stringify(response.user));
  }
  clear() { localStorage.clear(); }
}

const authStore = new AuthStore();
const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = authStore.token;
  return next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req);
};

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <ng-container *ngIf="auth.user as user; else publicShell">
      <aside class="sidebar">
        <strong>IPTV Manager</strong>
        <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
        <a routerLink="/lists" routerLinkActive="active">Listas</a>
        <a routerLink="/channels" routerLinkActive="active">Canais</a>
        <a routerLink="/favorites" routerLinkActive="active">Favoritos</a>
        <a *ngIf="user.role === 'ADMIN'" routerLink="/admin/users" routerLinkActive="active">Usuarios</a>
      </aside>
      <main class="shell">
        <header class="topbar">
          <span>{{ user.name }} · {{ user.role }}</span>
          <button type="button" (click)="logout()">Sair</button>
        </header>
        <router-outlet></router-outlet>
      </main>
    </ng-container>
    <ng-template #publicShell><router-outlet></router-outlet></ng-template>
  `
})
class AppComponent {
  auth = authStore;
  router = inject(Router);
  logout() { this.auth.clear(); this.router.navigateByUrl('/login'); }
}

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-page">
      <form class="panel" [formGroup]="form" (ngSubmit)="submit()">
        <h1>IPTV Manager</h1>
        <input formControlName="email" type="email" placeholder="Email">
        <input formControlName="password" type="password" placeholder="Senha">
        <button>Entrar</button>
        <a routerLink="/register">Criar conta</a>
        <small>{{ error }}</small>
      </form>
    </section>
  `
})
class LoginComponent {
  http = inject(HttpClient); router = inject(Router); fb = inject(FormBuilder);
  error = '';
  form = this.fb.group({ email: ['', [Validators.required, Validators.email]], password: ['', Validators.required] });
  submit() {
    if (this.form.invalid) return;
    this.http.post<AuthResponse>(`${API}/auth/login`, this.form.value).subscribe({
      next: r => { authStore.save(r); this.router.navigateByUrl('/dashboard'); },
      error: e => this.error = e.error?.message || 'Login invalido'
    });
  }
}

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-page">
      <form class="panel" [formGroup]="form" (ngSubmit)="submit()">
        <h1>Cadastro</h1>
        <input formControlName="name" placeholder="Nome">
        <input formControlName="email" type="email" placeholder="Email">
        <input formControlName="password" type="password" placeholder="Senha">
        <button>Cadastrar</button>
        <a routerLink="/login">Voltar ao login</a>
        <small>{{ error }}</small>
      </form>
    </section>
  `
})
class RegisterComponent {
  http = inject(HttpClient); router = inject(Router); fb = inject(FormBuilder);
  error = '';
  form = this.fb.group({ name: ['', Validators.required], email: ['', [Validators.required, Validators.email]], password: ['', [Validators.required, Validators.minLength(6)]] });
  submit() {
    if (this.form.invalid) return;
    this.http.post<AuthResponse>(`${API}/auth/register`, this.form.value).subscribe({
      next: r => { authStore.save(r); this.router.navigateByUrl('/dashboard'); },
      error: e => this.error = e.error?.message || 'Cadastro invalido'
    });
  }
}

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section>
      <h2>Dashboard</h2>
      <div class="metrics" *ngIf="dashboard">
        <article><span>{{ dashboard.totalLists }}</span><small>Listas</small></article>
        <article><span>{{ dashboard.totalChannels }}</span><small>Canais</small></article>
        <article><span>{{ dashboard.totalGroups }}</span><small>Categorias</small></article>
        <article><span>{{ dashboard.totalFavorites }}</span><small>Favoritos</small></article>
      </div>
      <div class="grid two">
        <section><h3>Listas recentes</h3><a *ngFor="let list of dashboard?.recentLists" [routerLink]="['/lists', list.id]">{{ list.name }} <small>{{ list.status }}</small></a></section>
        <section><h3>Canais recentes</h3><a *ngFor="let channel of dashboard?.recentChannels" [routerLink]="['/channels', channel.id, 'play']">{{ channel.name }} <small>{{ channel.groupTitle }}</small></a></section>
      </div>
    </section>
  `
})
class DashboardComponent {
  dashboard?: Dashboard;
  constructor(http: HttpClient) { http.get<Dashboard>(`${API}/dashboard`).subscribe(d => this.dashboard = d); }
}

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section>
      <div class="section-head"><h2>Listas IPTV</h2><a class="button" routerLink="/lists/new">Nova lista</a></div>
      <div *ngIf="error" class="notice error">{{ error }}</div>
      <table>
        <tr><th>Nome</th><th>Origem</th><th>Status</th><th>Canais</th><th></th></tr>
        <tr *ngFor="let list of lists">
          <td>{{ list.name }}</td><td>{{ list.sourceType }}</td><td>{{ list.status }}</td><td>{{ list.totalChannels }}</td>
          <td class="table-actions">
            <a [routerLink]="['/lists', list.id]">Abrir</a>
            <button type="button" class="danger" (click)="remove(list)">Remover</button>
          </td>
        </tr>
      </table>
    </section>
  `
})
class ListsComponent {
  http = inject(HttpClient);
  lists: IptvList[] = [];
  error = '';
  constructor() { this.load(); }
  load() {
    this.http.get<IptvList[]>(`${API}/iptv-lists`).subscribe(v => this.lists = v);
  }
  remove(list: IptvList) {
    if (!confirm(`Remover a lista "${list.name}" e todos os seus canais?`)) return;
    this.error = '';
    this.http.delete(`${API}/iptv-lists/${list.id}`).subscribe({
      next: () => this.lists = this.lists.filter(item => item.id !== list.id),
      error: e => this.error = e.error?.message || 'Nao foi possivel remover a lista.'
    });
  }
}

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section>
      <div class="section-head"><h2>Nova lista</h2><a routerLink="/lists">Voltar</a></div>
      <form class="panel narrow" [formGroup]="urlForm" (ngSubmit)="saveUrl()">
        <h3>URL remota</h3>
        <input formControlName="name" placeholder="Nome">
        <input formControlName="description" placeholder="Descricao">
        <input formControlName="sourceUrl" placeholder="https://...">
        <button type="submit" [disabled]="saving">{{ saving ? 'Salvando...' : 'Salvar URL' }}</button>
      </form>
      <form class="panel narrow" (submit)="upload($event)">
        <h3>Arquivo</h3>
        <input [formControl]="uploadName" placeholder="Nome">
        <input type="file" accept=".m3u,.m3u8" (change)="selectFile($event)">
        <small *ngIf="file">Selecionado: {{ file.name }}</small>
        <button type="submit" [disabled]="saving">{{ saving ? 'Enviando...' : 'Enviar arquivo' }}</button>
      </form>
      <div *ngIf="message" class="notice">{{ message }}</div>
      <div *ngIf="error" class="notice error">{{ error }}</div>
    </section>
  `
})
class NewListComponent {
  http = inject(HttpClient); router = inject(Router); fb = inject(FormBuilder);
  file?: File;
  saving = false;
  message = '';
  error = '';
  uploadName = this.fb.control('', Validators.required);
  urlForm = this.fb.group({ name: ['', Validators.required], description: [''], sourceUrl: ['', Validators.required] });
  saveUrl() {
    this.clearFeedback();
    if (this.urlForm.invalid) {
      this.error = 'Informe nome e uma URL iniciada por http:// ou https://.';
      this.urlForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.http.post<IptvList>(`${API}/iptv-lists/url`, this.urlForm.value).subscribe({
      next: l => {
        this.message = 'Lista salva. Abrindo detalhes para importar os canais.';
        this.router.navigate(['/lists', l.id]);
      },
      error: e => {
        this.saving = false;
        this.error = e.error?.message || 'Nao foi possivel salvar a lista por URL.';
      }
    });
  }
  selectFile(event: Event) {
    this.clearFeedback();
    this.file = (event.target as HTMLInputElement).files?.[0];
  }
  upload(event?: Event) {
    event?.preventDefault();
    this.clearFeedback();
    if (!this.uploadName.value) {
      this.error = 'Informe um nome para a lista.';
      return;
    }
    if (!this.file) {
      this.error = 'Selecione um arquivo .m3u ou .m3u8.';
      return;
    }
    if (!this.file.name.toLowerCase().endsWith('.m3u') && !this.file.name.toLowerCase().endsWith('.m3u8')) {
      this.error = 'O arquivo precisa ter extensao .m3u ou .m3u8.';
      return;
    }
    this.saving = true;
    const data = new FormData(); data.append('name', this.uploadName.value); data.append('file', this.file);
    this.http.post<IptvList>(`${API}/iptv-lists/upload`, data).subscribe({
      next: l => {
        this.message = 'Upload concluido. Abrindo detalhes para importar os canais.';
        this.router.navigate(['/lists', l.id]);
      },
      error: e => {
        this.saving = false;
        this.error = e.error?.message || 'Nao foi possivel enviar o arquivo.';
      }
    });
  }
  clearFeedback() {
    this.message = '';
    this.error = '';
  }
}

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section *ngIf="list">
      <div class="section-head"><h2>{{ list.name }}</h2><a routerLink="/lists">Voltar</a></div>
      <div class="panel">
        <p>{{ list.description }}</p>
        <p>Status: <strong>{{ list.status }}</strong> · Canais: {{ list.totalChannels }}</p>
        <p *ngIf="list.status === 'PROCESSING'"><small>Processando em segundo plano. A tela atualiza automaticamente.</small></p>
        <div *ngIf="message" class="notice">{{ message }}</div>
        <div *ngIf="error || list.errorMessage" class="notice error">{{ error || list.errorMessage }}</div>
        <div class="actions">
          <button (click)="importList()" [disabled]="importing || list.status === 'PROCESSING'">{{ importing ? 'Enfileirando...' : 'Importar' }}</button>
          <button *ngIf="list.sourceType === 'URL'" (click)="refresh()" [disabled]="refreshing || list.status === 'PROCESSING'">{{ refreshing ? 'Enfileirando...' : 'Atualizar' }}</button>
          <a [routerLink]="['/channels']" [queryParams]="{ listId: list.id }">Ver canais</a>
          <button type="button" class="danger" (click)="removeList()" [disabled]="removing">Remover lista</button>
        </div>
      </div>
    </section>
  `
})
class ListDetailComponent {
  http = inject(HttpClient); router = inject(Router);
  list?: IptvList;
  importing = false;
  refreshing = false;
  removing = false;
  message = '';
  error = '';
  pollTimer?: number;
  constructor() { this.load(); }
  id() { return location.pathname.split('/').pop(); }
  load() {
    this.http.get<IptvList>(`${API}/iptv-lists/${this.id()}`).subscribe({
      next: l => {
        this.list = l;
        this.schedulePollIfProcessing();
      },
      error: e => this.error = e.error?.message || 'Nao foi possivel carregar a lista.'
    });
  }
  importList() {
    this.runImport(`${API}/iptv-lists/${this.id()}/import`, 'Importacao iniciada em segundo plano.', 'importing');
  }
  refresh() {
    this.runImport(`${API}/iptv-lists/${this.id()}/refresh`, 'Atualizacao iniciada em segundo plano.', 'refreshing');
  }
  removeList() {
    if (!this.list || !confirm(`Remover a lista "${this.list.name}" e todos os seus canais?`)) return;
    this.removing = true;
    this.error = '';
    this.http.delete(`${API}/iptv-lists/${this.id()}`).subscribe({
      next: () => this.router.navigateByUrl('/lists'),
      error: e => {
        this.removing = false;
        this.error = e.error?.message || 'Nao foi possivel remover a lista.';
      }
    });
  }
  runImport(url: string, successMessage: string, flag: 'importing' | 'refreshing') {
    this[flag] = true;
    this.message = '';
    this.error = '';
    this.http.post<IptvList>(url, {}).subscribe({
      next: l => {
        this[flag] = false;
        this.list = l;
        this.message = l.status === 'PROCESSING' ? successMessage : (l.status === 'IMPORTED' ? 'Importacao concluida.' : '');
        this.error = l.status === 'ERROR' ? (l.errorMessage || 'Nao foi possivel importar a lista.') : '';
        this.schedulePollIfProcessing();
      },
      error: e => {
        this[flag] = false;
        this.error = e.error?.message || 'Nao foi possivel importar a lista.';
      }
    });
  }
  schedulePollIfProcessing() {
    if (this.list?.status !== 'PROCESSING' || this.pollTimer) return;
    this.pollTimer = window.setInterval(() => {
      this.http.get<IptvList>(`${API}/iptv-lists/${this.id()}`).subscribe(l => {
        this.list = l;
        if (l.status !== 'PROCESSING' && this.pollTimer) {
          window.clearInterval(this.pollTimer);
          this.pollTimer = undefined;
          this.message = l.status === 'IMPORTED' ? 'Importacao concluida.' : '';
          this.error = l.status === 'ERROR' ? (l.errorMessage || 'Nao foi possivel importar a lista.') : '';
        }
      });
    }, 3000);
  }
}

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  template: `
    <section>
      <div class="section-head">
        <div>
          <h2>Canais</h2>
          <small *ngIf="page">{{ page.totalElements }} canais encontrados · pagina {{ page.number + 1 }} de {{ page.totalPages || 1 }}</small>
        </div>
        <div class="toolbar">
          <input [formControl]="term" placeholder="Buscar">
          <select [formControl]="testStatus">
            <option value="">Todos os testes</option>
            <option value="UNTESTED">Nao testados</option>
            <option value="ONLINE">Online</option>
            <option value="OFFLINE">Offline</option>
            <option value="INVALID">Invalidos</option>
            <option value="UNKNOWN">Indefinidos</option>
          </select>
          <button type="button" (click)="testBatch()" [disabled]="batchTesting || !page?.totalElements">
            {{ batchTesting ? 'Testando...' : 'Testar filtrados' }}
          </button>
        </div>
      </div>
      <div *ngIf="batchMessage" class="notice">{{ batchMessage }}</div>

      <div class="catalog-layout">
        <aside class="category-panel">
          <button type="button" [class.ghost]="selectedGroup" (click)="selectGroup('')">Todas</button>
          <button type="button" *ngFor="let group of groups" [class.ghost]="selectedGroup !== group" (click)="selectGroup(group)">
            {{ group }}
          </button>
        </aside>

        <div>
          <div class="channels">
            <article *ngFor="let channel of page?.content">
              <img [src]="channel.logoUrl || ''" [alt]="channel.name" (error)="hideBrokenImage($event)">
              <div>
                <strong>{{ channel.name }}</strong>
                <small>Lista: {{ channel.iptvListName }}</small>
                <small>Categoria: {{ channel.groupTitle }}</small>
                <small *ngIf="testResults[channel.id]; else lastTest" [class]="testClass(testResults[channel.id].status)">
                  {{ testResults[channel.id].message }}
                </small>
                <ng-template #lastTest>
                  <small *ngIf="channel.testStatus" [class]="testClass(channel.testStatus)">
                    Ultimo teste: {{ channel.testStatus }} · {{ channel.testMessage }}
                  </small>
                </ng-template>
              </div>
              <button type="button" class="ghost" (click)="test(channel)" [disabled]="testing[channel.id]">
                {{ testing[channel.id] ? '...' : 'Testar' }}
              </button>
              <button type="button" (click)="toggle(channel)">{{ channel.favorite ? '★' : '☆' }}</button>
              <button type="button" class="danger icon-action" (click)="remove(channel)">Excluir</button>
              <a [routerLink]="['/channels', channel.id, 'play']">▶</a>
            </article>
          </div>

          <div class="pager" *ngIf="page">
            <button type="button" (click)="previous()" [disabled]="page.number === 0">Anterior</button>
            <span>{{ page.number + 1 }} / {{ page.totalPages || 1 }}</span>
            <button type="button" (click)="next()" [disabled]="page.number + 1 >= page.totalPages">Proxima</button>
          </div>
        </div>
      </div>
    </section>
  `
})
class ChannelsComponent {
  http = inject(HttpClient); fb = inject(FormBuilder);
  term = this.fb.control('');
  testStatus = this.fb.control('');
  page?: Page<Channel>;
  groups: string[] = [];
  selectedGroup = '';
  pageIndex = 0;
  pageSize = 100;
  testing: Record<number, boolean> = {};
  testResults: Record<number, ChannelTestResult> = {};
  batchTesting = false;
  batchMessage = '';
  batchPollTimer?: number;
  batchPolls = 0;
  constructor() {
    this.loadGroups();
    this.load();
    this.term.valueChanges.subscribe(() => {
      this.pageIndex = 0;
      this.load();
    });
    this.testStatus.valueChanges.subscribe(() => {
      this.pageIndex = 0;
      this.load();
    });
  }
  load() {
    const params = new URLSearchParams(location.search);
    const listId = params.get('listId');
    const apiParams = new URLSearchParams();
    if (this.term.value) apiParams.set('term', this.term.value);
    if (listId) apiParams.set('listId', listId);
    if (this.selectedGroup) apiParams.set('group', this.selectedGroup);
    if (this.testStatus.value) apiParams.set('testStatus', this.testStatus.value);
    apiParams.set('page', String(this.pageIndex));
    apiParams.set('size', String(this.pageSize));
    this.http.get<Page<Channel>>(`${API}/channels?${apiParams.toString()}`).subscribe(p => this.page = p);
  }
  loadGroups() {
    const params = new URLSearchParams(location.search);
    const listId = params.get('listId');
    const query = listId ? `?listId=${encodeURIComponent(listId)}` : '';
    this.http.get<string[]>(`${API}/channels/groups${query}`).subscribe(groups => this.groups = groups);
  }
  selectGroup(group: string) {
    this.selectedGroup = group;
    this.pageIndex = 0;
    this.load();
  }
  previous() {
    if (!this.page || this.page.number === 0) return;
    this.pageIndex = this.page.number - 1;
    this.load();
  }
  next() {
    if (!this.page || this.page.number + 1 >= this.page.totalPages) return;
    this.pageIndex = this.page.number + 1;
    this.load();
  }
  toggle(channel: Channel) {
    const req: Observable<Channel> = channel.favorite
      ? this.http.delete<Channel>(`${API}/channels/${channel.id}/favorite`)
      : this.http.post<Channel>(`${API}/channels/${channel.id}/favorite`, {});
    req.subscribe(updated => channel.favorite = updated.favorite);
  }
  test(channel: Channel) {
    this.testing[channel.id] = true;
    this.http.post<ChannelTestResult>(`${API}/channels/${channel.id}/test`, {}).subscribe({
      next: result => {
        this.testing[channel.id] = false;
        this.testResults[channel.id] = result;
        channel.testStatus = result.status;
        channel.testHttpStatus = result.httpStatus;
        channel.testMessage = result.message;
        channel.lastTestAt = new Date().toISOString();
      },
      error: e => {
        this.testing[channel.id] = false;
        this.testResults[channel.id] = {
          channelId: channel.id,
          status: 'UNKNOWN',
          message: e.error?.message || 'Nao foi possivel testar o canal'
        };
      }
    });
  }
  testBatch() {
    const params = new URLSearchParams(location.search);
    const listId = params.get('listId');
    const apiParams = new URLSearchParams();
    if (this.term.value) apiParams.set('term', this.term.value);
    if (listId) apiParams.set('listId', listId);
    if (this.selectedGroup) apiParams.set('group', this.selectedGroup);
    if (this.testStatus.value) apiParams.set('testStatus', this.testStatus.value);
    this.batchTesting = true;
    this.batchMessage = '';
    this.http.post<ChannelTestBatch>(`${API}/channels/test-batch?${apiParams.toString()}`, {}).subscribe({
      next: batch => {
        this.batchMessage = batch.totalChannels
          ? `${batch.message}: ${batch.totalChannels} canais na fila.`
          : batch.message;
        this.startBatchPolling(batch.totalChannels);
      },
      error: e => {
        this.batchTesting = false;
        this.batchMessage = e.error?.message || 'Nao foi possivel iniciar o teste em lote.';
      }
    });
  }
  startBatchPolling(totalChannels: number) {
    if (!totalChannels) {
      this.batchTesting = false;
      return;
    }
    if (this.batchPollTimer) window.clearInterval(this.batchPollTimer);
    this.batchPolls = 0;
    this.batchPollTimer = window.setInterval(() => {
      this.batchPolls++;
      this.load();
      if (this.batchPolls >= 30 && this.batchPollTimer) {
        window.clearInterval(this.batchPollTimer);
        this.batchPollTimer = undefined;
        this.batchTesting = false;
        this.batchMessage = 'Teste em lote enviado. Atualize os filtros para conferir os resultados finais.';
      }
    }, 4000);
  }
  testClass(status: ChannelTestStatus) {
    return 'test-status ' + status.toLowerCase();
  }
  remove(channel: Channel) {
    if (!confirm(`Remover o canal "${channel.name}" desta lista?`)) return;
    this.http.delete(`${API}/channels/${channel.id}`).subscribe({
      next: () => {
        if (this.page) {
          this.page = {
            ...this.page,
            totalElements: Math.max(0, this.page.totalElements - 1),
            content: this.page.content.filter(item => item.id !== channel.id)
          };
        }
      },
      error: e => {
        this.testResults[channel.id] = {
          channelId: channel.id,
          status: 'UNKNOWN',
          message: e.error?.message || 'Nao foi possivel remover o canal'
        };
      }
    });
  }
  hideBrokenImage(event: Event) {
    (event.target as HTMLImageElement).style.visibility = 'hidden';
  }
}

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section><h2>Favoritos</h2>
      <div class="channels">
        <article *ngFor="let channel of page?.content">
          <img [src]="channel.logoUrl || ''" alt="">
          <div><strong>{{ channel.name }}</strong><small>{{ channel.groupTitle }}</small></div>
          <a [routerLink]="['/channels', channel.id, 'play']">▶</a>
        </article>
      </div>
    </section>
  `
})
class FavoritesComponent {
  page?: Page<Channel>;
  constructor(http: HttpClient) { http.get<Page<Channel>>(`${API}/channels/favorites?size=50`).subscribe(p => this.page = p); }
}

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="player" *ngIf="channel">
      <video #video controls autoplay></video>
      <aside>
        <a routerLink="/channels">Voltar</a>
        <h2>{{ channel.name }}</h2>
        <p>{{ channel.groupTitle }}</p>
        <small>{{ error }}</small>
      </aside>
    </section>
  `
})
class PlayerComponent {
  http = inject(HttpClient);
  channel?: Channel; error = ''; hls?: Hls;
  constructor() {
    const id = location.pathname.split('/').at(-2);
    this.http.get<Channel>(`${API}/channels/${id}`).subscribe(c => { this.channel = c; setTimeout(() => this.play(c.streamUrl)); });
  }
  play(url: string) {
    const video = document.querySelector('video') as HTMLVideoElement | null;
    if (!video) return;
    this.hls?.destroy();
    if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = url;
    } else if (Hls.isSupported()) {
      this.hls = new Hls();
      this.hls.loadSource(url);
      this.hls.attachMedia(video);
      this.hls.on(Hls.Events.ERROR, () => this.error = 'Nao foi possivel carregar o stream.');
    } else {
      this.error = 'Seu navegador nao suporta HLS.';
    }
  }
}

@Component({
  standalone: true,
  imports: [CommonModule],
  template: `
    <section><h2>Usuarios</h2>
      <table>
        <tr><th>Nome</th><th>Email</th><th>Perfil</th><th>Ativo</th></tr>
        <tr *ngFor="let user of users"><td>{{ user.name }}</td><td>{{ user.email }}</td><td>{{ user.role }}</td><td>{{ user.active }}</td></tr>
      </table>
    </section>
  `
})
class UsersComponent {
  users: User[] = [];
  constructor(http: HttpClient) { http.get<User[]>(`${API}/users`).subscribe(u => this.users = u); }
}

const guard = () => authStore.token ? true : inject(Router).parseUrl('/login');
const adminGuard = () => authStore.user?.role === 'ADMIN' ? true : inject(Router).parseUrl('/dashboard');
const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [guard] },
  { path: 'lists', component: ListsComponent, canActivate: [guard] },
  { path: 'lists/new', component: NewListComponent, canActivate: [guard] },
  { path: 'lists/:id', component: ListDetailComponent, canActivate: [guard] },
  { path: 'channels', component: ChannelsComponent, canActivate: [guard] },
  { path: 'channels/:id/play', component: PlayerComponent, canActivate: [guard] },
  { path: 'favorites', component: FavoritesComponent, canActivate: [guard] },
  { path: 'admin/users', component: UsersComponent, canActivate: [guard, adminGuard] },
  { path: '**', redirectTo: 'dashboard' }
];

const config: ApplicationConfig = {
  providers: [provideRouter(routes), provideHttpClient(withInterceptors([authInterceptor]))]
};

bootstrapApplication(AppComponent, config).catch(console.error);
