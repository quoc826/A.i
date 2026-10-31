(() => {
  'use strict';
  const fullPage = document.querySelector('#shopping-chat');
  const root = fullPage || document.createElement('section');
  let launcher;
  if (!fullPage) {
    root.className = 'chat-widget'; root.hidden = true; root.id = 'shopping-chat';
    launcher = document.createElement('button'); launcher.className = 'chat-launcher';
    launcher.textContent = '✦ Hỏi eShopping'; launcher.setAttribute('aria-controls', root.id);
    launcher.setAttribute('aria-expanded', 'false');
    document.body.append(root, launcher);
    launcher.onclick = () => {
      root.hidden = !root.hidden; launcher.setAttribute('aria-expanded', String(!root.hidden));
      if (!root.hidden) input.focus();
    };
  }
  // This template is static. All server/model/user strings below are rendered using textContent.
  root.innerHTML = `
    <header class="chat-header"><div><strong>✦ eShopping</strong><span class="chat-state">Đang kết nối…</span></div>
    <div class="chat-actions"><button type="button" class="chat-reset" aria-label="Xóa cuộc trò chuyện" title="Cuộc trò chuyện mới">↻</button>
    <button type="button" class="chat-close" aria-label="Đóng chat">×</button></div></header>
    <div class="chat-messages" role="log" aria-live="polite" aria-relevant="additions" tabindex="0"></div>
    <div class="chat-suggestions"><button type="button">Tìm tai nghe Sony</button><button type="button">Laptop học lập trình</button><button type="button">Đơn hàng của tôi</button></div>
    <form class="chat-compose">
      <div class="chat-filters"><label>Giá tối đa (USD)<input class="chat-price" type="number" min="0.01" step="0.01" max="999999999999" placeholder="Không giới hạn"></label>
      <label class="check-label"><input class="chat-stock" type="checkbox"> Chỉ còn hàng</label></div>
      <div class="chat-input-row"><textarea maxlength="2000" rows="2" required aria-label="Tin nhắn" placeholder="Bạn đang cần tìm sản phẩm gì?"></textarea><button class="chat-send" type="submit" aria-label="Gửi tin nhắn">↑</button></div>
      <p class="chat-error" role="alert" hidden></p><p class="chat-footnote">Giá và tồn kho có thể thay đổi. Kiểm tra tại trang sản phẩm trước khi mua.</p>
    </form>`;
  const log = root.querySelector('.chat-messages'), input = root.querySelector('textarea');
  const send = root.querySelector('.chat-send'), form = root.querySelector('form');
  const error = root.querySelector('.chat-error'), state = root.querySelector('.chat-state');
  const reset = root.querySelector('.chat-reset');
  let busy = false;
  root.querySelector('.chat-close').hidden = Boolean(fullPage);
  root.querySelector('.chat-close').onclick = () => {root.hidden = true; launcher?.setAttribute('aria-expanded','false'); launcher?.focus();};
  root.addEventListener('keydown', event => {if (event.key === 'Escape' && !fullPage) root.querySelector('.chat-close').click();});
  function element(tag, className, text) {
    const node = document.createElement(tag); if (className) node.className = className;
    if (text !== undefined) node.textContent = text; return node;
  }
  const money = value => new Intl.NumberFormat('en-US', {style:'currency', currency:'USD'}).format(value);
  function append(role, text, products = [], orders = [], mode = '') {
    const bubble = element('div', 'chat-message ' + (role === 'user' ? 'from-user' : 'from-assistant'));
    bubble.append(element('span','chat-speaker',role === 'user' ? 'Bạn' : 'eShopping'));
    bubble.append(element('p','chat-text',text));
    if (products.length) {
      const cards = element('div','chat-products');
      products.forEach(p => {
        const card = element('a','chat-product'); card.href = '/product/' + encodeURIComponent(p.id);
        card.append(element('strong','',p.name));
        if (p.description) card.append(element('span','chat-product-description',p.description.slice(0,150)));
        card.append(element('span','chat-product-price',money(p.price)));
        card.append(element('span','chat-product-stock',p.quantity > 0 ? 'Còn ' + p.quantity + ' sản phẩm · Xem chi tiết →' : 'Tạm hết hàng'));
        cards.append(card);
      });bubble.append(cards);
    }
    orders.forEach(order => {
      const link = element('a','chat-order','#' + order.id + ' · ' + order.date + ' · ' + order.status + ' · ' + money(order.total));
      link.href = '/order/' + encodeURIComponent(order.id);bubble.append(link);
    });
    if (role !== 'user' && mode) bubble.append(element('small','chat-mode',mode === 'AI' ? 'Trợ lý AI' : 'Tra cứu danh mục'));
    log.append(bubble);log.scrollTop = log.scrollHeight;return bubble;
  }
  function welcome() {
    append('assistant','Chào bạn! Mình có thể giúp tìm sản phẩm theo nhu cầu, xem giá và tồn kho, hoặc tra cứu đơn hàng của bạn. Bạn đang tìm món đồ nào?');
  }
  async function request(method, body) {
    const response = await fetch('/api/v1/chat', {
      method, credentials: 'same-origin', signal: AbortSignal.timeout(90000),
      headers: {'Content-Type':'application/json',
        [document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN']:
          document.querySelector('meta[name="_csrf"]')?.content || ''},
      ...(body ? {body:JSON.stringify(body)} : {})
    });
    if (!response.ok) {
      let message = response.status === 403 ? 'Phiên làm việc đã hết hạn. Vui lòng tải lại trang.' : 'Chưa gửi được tin nhắn. Vui lòng thử lại.';
      try {const data = await response.json(); message = data.message || message;} catch (_) {}
      throw new Error(message);
    }
    return response.status === 204 ? null : response.json();
  }
  function showError(message) {error.hidden = false; error.textContent = message;}
  function setBusy(value) {busy = value;send.disabled = value;reset.disabled = value;input.readOnly = value;form.setAttribute('aria-busy',String(value));}
  async function submit() {
    const question = input.value.trim();
    if (!question || busy || !form.reportValidity()) return;
    error.hidden = true;setBusy(true);input.value = '';
    const userBubble = append('user',question);
    const typing = append('assistant','Đang tìm lựa chọn cho bạn…');typing.classList.add('chat-typing');
    const max = root.querySelector('.chat-price').value;
    try {
      const reply = await request('POST',{message:question,maxPrice:max ? Number(max) : null,inStock:root.querySelector('.chat-stock').checked});
      typing.remove();append('assistant',reply.text,reply.products,reply.orders,reply.mode);
      state.textContent = reply.mode === 'AI' ? 'Trợ lý AI · Sẵn sàng' : 'Tra cứu danh mục · Sẵn sàng';
      if (reply.searchMode !== 'CATALOG') window.shopTrack?.('SEARCH',{keyword:question,resultCount:reply.products.length});
    } catch (e) {
      typing.remove();userBubble.remove();input.value = question;
      showError(e.name === 'TimeoutError' ? 'Phản hồi đang chậm. Bạn hãy thử lại sau.' : e.message);
    } finally {setBusy(false);input.focus();}
  }
  form.addEventListener('submit',event => {event.preventDefault();submit();});
  input.addEventListener('keydown',event => {if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {event.preventDefault();submit();}});
  root.querySelectorAll('.chat-suggestions button').forEach(button => {button.onclick = () => {if (!busy) {input.value = button.textContent;submit();}};});
  reset.onclick = async () => {
    if (busy) return;setBusy(true);
    try {await request('DELETE');log.replaceChildren();error.hidden = true;welcome();}
    catch (e) {showError(e.message);}
    finally {setBusy(false);}
  };
  setBusy(true);
  request('GET').then(data => {
    state.textContent = data.aiEnabled ? 'Trợ lý AI · Sẵn sàng' : 'Tra cứu danh mục · Sẵn sàng';
    if (!data.messages.length) welcome();
    else data.messages.forEach(m => append(m.role,m.text,m.products,m.orders,m.mode));
  }).catch(() => {state.textContent = 'Chưa kết nối';welcome();showError('Không tải được cuộc trò chuyện. Hãy tải lại trang trước khi gửi.');}).finally(() => setBusy(false));
})();
