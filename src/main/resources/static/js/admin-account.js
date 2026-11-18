const adminAccountForm = document.querySelector('#admin-account-form');
if (adminAccountForm) {
    const fields = adminAccountForm.elements;
    const customer = adminAccountForm.querySelector('[data-customer]');
    const expert = adminAccountForm.querySelector('[data-expert]');
    function updateAccountFields() {
        const isExpert = fields.kind.value === 'CHUYEN_GIA';
        customer.hidden = customer.disabled = isExpert;
        expert.hidden = expert.disabled = !isExpert;
        fields.tenToChuc.required = !isExpert && fields.loaiKhachHang.value === 'TO_CHUC';
        fields.linhVuc.required = fields.gioiThieu.required = isExpert;
    }
    fields.kind.addEventListener('change', updateAccountFields);
    fields.loaiKhachHang.addEventListener('change', updateAccountFields);
    updateAccountFields();
}

for (const upload of document.querySelectorAll('[data-avatar-upload]')) {
    const input = upload.querySelector('input[type="file"]');
    const preview = upload.querySelector('[data-avatar-preview]');
    const image = preview.querySelector('img');
    const error = upload.querySelector('[data-avatar-error]');
    const reject = (message) => {
        input.setCustomValidity(message);
        error.textContent = message;
        error.hidden = false;
        preview.hidden = true;
    };
    input.addEventListener('change', () => {
        const file = input.files[0];
        input.setCustomValidity('');
        error.hidden = true;
        preview.hidden = true;
        image.removeAttribute('src');
        if (!file) return;
        if (file.size > 2 * 1024 * 1024) {
            reject('Ảnh đại diện không được vượt quá 2 MB.');
            return;
        }
        const reader = new FileReader();
        reader.addEventListener('load', () => {
            if (input.files[0] !== file) return;
            image.onload = () => { if (input.files[0] === file) preview.hidden = false; };
            image.onerror = () => {
                if (input.files[0] === file) reject('Không xem được ảnh. Hãy chọn tệp JPEG, PNG hoặc WebP.');
            };
            image.src = reader.result;
        });
        reader.addEventListener('error', () => {
            if (input.files[0] === file) reject('Không đọc được tệp ảnh.');
        });
        reader.readAsDataURL(file);
    });
}
