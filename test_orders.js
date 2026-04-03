async function test() {
  try {
    const loginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        phone: '13800000002',
        password: '123456'
      })
    });
    console.log('Login status:', loginRes.status);
    const loginText = await loginRes.text();
    // console.log('Login response:', loginText);
    const loginData = JSON.parse(loginText);
    
    if (!loginData.data || !loginData.data.token) {
        console.error("Login failed:", loginData);
        return;
    }
    const token = loginData.data.token;
    console.log('Token obtained');

    const ordersRes = await fetch('http://localhost:8080/api/orders', {
      headers: { Authorization: `Bearer ${token}` }
    });
    const ordersText = await ordersRes.text();
    console.log('Orders response:', ordersText);
  } catch (err) {
    console.error('Error:', err);
  }
}

test();