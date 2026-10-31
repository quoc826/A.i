(() => {
  'use strict';
  const csrf = () => ({
    [document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN']:
      document.querySelector('meta[name="_csrf"]')?.content || ''
  });
  window.shopTrack = async (eventType, payload = {}) => {
    try {
      await fetch('/api/v1/tracking/events', {
        method: 'POST', credentials: 'same-origin', keepalive: true,
        headers: {'Content-Type': 'application/json', ...csrf()},
        signal: AbortSignal.timeout(2000),
        body: JSON.stringify({
          eventId: crypto.randomUUID(), eventType, path: location.pathname.slice(0, 500),
          title: document.title.slice(0, 200), timestamp: new Date().toISOString(), ...payload
        })
      });
    } catch (_) { /* Tracking must never prevent shopping. */ }
  };
  window.shopTrack('PAGE_VIEW');
  const product = document.querySelector('#product-context');
  if (product) window.shopTrack('PRODUCT_VIEW', {productId: Number(product.dataset.productId)});
  const search = document.querySelector('#search-context');
  if (search) window.shopTrack('SEARCH', {keyword: search.dataset.keyword, resultCount: Number(search.dataset.resultCount)});
  document.querySelectorAll('form[action="/cart/add"]').forEach(form => {
    form.addEventListener('submit', async event => {
      event.preventDefault();
      const button = form.querySelector('button');
      if (button.disabled) return;
      button.disabled = true;
      const data = new FormData(form);
      try {
        const response = await fetch(form.action, {method: 'POST', body: data, credentials: 'same-origin', headers: csrf()});
        const destination = new URL(response.url);
        if (response.ok && response.redirected && destination.pathname === '/cart') {
          await window.shopTrack('ADD_TO_CART', {productId: Number(data.get('productId')), quantity: Number(data.get('quantity'))});
          location.assign('/cart');
        } else if (response.redirected && destination.pathname === '/login') {
          location.assign('/login');
        } else {
          let error = form.querySelector('.cart-error');
          if (!error) {error = document.createElement('p'); error.className = 'cart-error error'; error.setAttribute('role', 'alert'); form.append(error);}
          error.textContent = 'Chưa thêm được vào giỏ. Hãy kiểm tra số lượng và thử lại.';
        }
      } catch (_) {
        let error = form.querySelector('.cart-error');
        if (!error) {error = document.createElement('p'); error.className = 'cart-error error'; form.append(error);}
        error.textContent = 'Không kết nối được cửa hàng. Vui lòng thử lại.';
      } finally {button.disabled = false;}
    });
  });
})();
