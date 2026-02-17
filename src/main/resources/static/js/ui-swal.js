(function () {
    function csrfHeaders() {
        var tokenMeta = document.querySelector('meta[name=\"_csrf\"]');
        var headerMeta = document.querySelector('meta[name=\"_csrf_header\"]');
        if (!tokenMeta || !headerMeta) {
            return {};
        }
        var headers = {};
        headers[headerMeta.getAttribute('content')] = tokenMeta.getAttribute('content');
        return headers;
    }

    function toast(icon, title) {
        return Swal.fire({
            toast: true,
            position: 'top-end',
            icon: icon,
            title: title,
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true
        });
    }

    window.uiToastSuccess = function (title) {
        return toast('success', title || 'Operação realizada com sucesso.');
    };

    window.uiToastError = function (title) {
        return toast('error', title || 'Não foi possível concluir a operação.');
    };

    window.uiConfirmDelete = function (options) {
        var opts = options || {};
        return Swal.fire({
            title: opts.title || 'Confirmar exclusão',
            text: opts.text || 'Esta ação não pode ser desfeita.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: opts.confirmText || 'Excluir',
            cancelButtonText: opts.cancelText || 'Cancelar',
            reverseButtons: true
        }).then(function (r) {
            return r.isConfirmed;
        });
    };

    window.uiFetchForm = function (url, method, formData) {
        var headers = Object.assign({
            'X-Requested-With': 'XMLHttpRequest'
        }, csrfHeaders());

        return fetch(url, {
            method: method || 'POST',
            headers: Object.assign({
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            }, headers),
            body: new URLSearchParams(formData).toString()
        }).then(function (r) {
            return r.json().catch(function () {
                return { ok: false, message: 'Resposta inválida do servidor.' };
            });
        });
    };

    window.uiSubmitFromSwal = function (url, method, formSelector) {
        var container = Swal.getHtmlContainer();
        if (!container) {
            return Promise.resolve({ ok: false, message: 'Modal não está aberto.' });
        }
        var form = container.querySelector(formSelector);
        if (!form) {
            return Promise.resolve({ ok: false, message: 'Formulário não encontrado.' });
        }

        var formData = {};
        var elements = form.querySelectorAll('input, select, textarea');
        for (var i = 0; i < elements.length; i++) {
            var el = elements[i];
            if (!el.name) {
                continue;
            }
            if ((el.type === 'checkbox' || el.type === 'radio') && !el.checked) {
                continue;
            }
            formData[el.name] = el.value;
        }
        return window.uiFetchForm(url, method, formData);
    };

    document.addEventListener('DOMContentLoaded', function () {
        var flash = document.getElementById('swal-flash');
        if (flash) {
            var success = flash.getAttribute('data-success');
            var error = flash.getAttribute('data-error');
            var warning = flash.getAttribute('data-warning');
            if (success) {
                window.uiToastSuccess(success);
                return;
            } else if (error) {
                window.uiToastError(error);
                return;
            } else if (warning) {
                toast('warning', warning);
                return;
            }
        }
        if (typeof window.uiHandleLoginFlashFromQuery === 'function') {
            window.uiHandleLoginFlashFromQuery();
        }
    });

    function digitsOnly(s) {
        return (s || '').replace(/\D+/g, '');
    }

    window.uiFormatCpfCnpj = function (v) {
        var s = digitsOnly(v);
        if (s.length === 11) {
            return s.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
        }
        if (s.length === 14) {
            return s.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3\/$4-$5');
        }
        return v || '';
    };

    window.uiMaskCpfCnpjInput = function (inputEl) {
        if (!inputEl) return;
        inputEl.addEventListener('input', function () {
            var s = digitsOnly(inputEl.value);
            if (s.length <= 11) {
                // CPF progressivo
                var parts = [];
                parts.push(s.substring(0, 3));
                if (s.length > 3) parts.push(s.substring(3, 6));
                if (s.length > 6) parts.push(s.substring(6, 9));
                var last = s.substring(9, 11);
                var joined = parts.filter(Boolean).join('.');
                if (s.length > 9) {
                    joined = joined + '-' + last;
                }
                inputEl.value = joined;
            } else {
                // CNPJ progressivo
                var p1 = s.substring(0, 2);
                var p2 = s.substring(2, 5);
                var p3 = s.substring(5, 8);
                var p4 = s.substring(8, 12);
                var p5 = s.substring(12, 14);
                var out = p1;
                if (p2) out += '.' + p2;
                if (p3) out += '.' + p3;
                if (p4) out += '/' + p4;
                if (p5) out += '-' + p5;
                inputEl.value = out;
            }
        });
    };

    window.uiApplyCpfCnpjFormattingIn = function (container) {
        var scope = container || document;
        var nodes = scope.querySelectorAll('.cpfcnpj-cell');
        for (var i = 0; i < nodes.length; i++) {
            var n = nodes[i];
            n.textContent = window.uiFormatCpfCnpj(n.textContent);
        }
    };

    window.uiFormatPhoneBR = function (v) {
        var s = digitsOnly(v);
        if (s.length <= 0) return '';
        if (s.length <= 10) {
            return s.replace(/(\d{0,2})(\d{0,4})(\d{0,4})/, function(_, ddd, p1, p2) {
                var out = '';
                if (ddd) out += '(' + ddd + ')';
                if (p1) out += (out ? ' ' : '') + p1;
                if (p2) out += '-' + p2;
                return out;
            });
        }
        return s.replace(/(\d{2})(\d{5})(\d{4}).*/, '($1) $2-$3');
    };

    window.uiMaskPhoneInput = function (inputEl) {
        if (!inputEl) return;
        inputEl.addEventListener('input', function () {
            var pos = inputEl.selectionStart;
            inputEl.value = window.uiFormatPhoneBR(inputEl.value);
            inputEl.setSelectionRange(inputEl.value.length, inputEl.value.length);
        });
    };

    window.uiApplyPhoneFormattingIn = function (container) {
        var scope = container || document;
        var nodes = scope.querySelectorAll('.phone-cell');
        for (var i = 0; i < nodes.length; i++) {
            var n = nodes[i];
            n.textContent = window.uiFormatPhoneBR(n.textContent);
        }
    };

    window.uiHandleLoginFlashFromQuery = function () {
        try {
            var p = new URLSearchParams(window.location.search);
            if (p.has('error')) {
                window.uiToastError('Usuário ou senha inválidos.');
            } else if (p.has('logout')) {
                window.uiToastSuccess('Você saiu com sucesso.');
            } else if (p.get('login') === 'success') {
                window.uiToastSuccess('Login realizado com sucesso!');
                // Limpa o parâmetro da URL para não exibir novamente ao recarregar
                window.history.replaceState({}, document.title, window.location.pathname);
            }
        } catch (e) {}
    };

    window.uiMaskMoneyInput = function (inputEl) {
        if (!inputEl) return;
        function format(v) {
            var s = digitsOnly(v);
            if (!s) return 'R$ 0,00';
            while (s.length < 3) s = '0' + s;
            var cents = s.slice(-2);
            var ints = s.slice(0, -2).replace(/^0+(?=\d)/, '');
            if (ints.length === 0) ints = '0';
            ints = ints.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
            return 'R$ ' + ints + ',' + cents;
        }
        inputEl.addEventListener('input', function () {
            inputEl.value = format(inputEl.value);
        });
        inputEl.value = format(inputEl.value);
    };

    window.uiMaskProcessNumberInput = function (inputEl) {
        if (!inputEl) return;
        inputEl.addEventListener('input', function () {
            var s = digitsOnly(inputEl.value).slice(0, 25);
            var p1 = s.substring(0, 7);
            var p2 = s.substring(7, 9);
            var p3 = s.substring(9, 13);
            var p4 = s.substring(13, 14);
            var p5 = s.substring(14, 16);
            var p6 = s.substring(16, 20);
            var out = p1;
            if (p2) out += '-' + p2;
            if (p3) out += '.' + p3;
            if (p4) out += '.' + p4;
            if (p5) out += '.' + p5;
            if (p6) out += '.' + p6;
            inputEl.value = out;
        });
    };
})();
