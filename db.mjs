import mysql from 'mysql2/promise';

const config = {
  host: process.env.DB_HOST || '127.0.0.1',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'testpilot',
};

export async function createDb() {
  let boot;
  try {
    boot = await mysql.createConnection({ host: config.host, port: config.port, user: config.user, password: config.password });
  } catch (error) {
    throw new Error(`无法连接 MySQL（${config.host}:${config.port}，用户 ${config.user}）：${error.code || error.message}。请确认 MySQL 服务已启动、账号密码正确；也可通过 DB_HOST / DB_PORT / DB_USER / DB_PASSWORD / DB_NAME 环境变量覆盖默认连接。`);
  }
  await boot.query(`CREATE DATABASE IF NOT EXISTS \`${config.database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`);
  await boot.end();
  const pool = mysql.createPool(config);
  const query = async (sql, params = []) => {
    const [rows] = await pool.query(sql, params);
    return rows;
  };
  return {
    one: async (sql, ...params) => (await query(sql, params))[0] || null,
    all: (sql, ...params) => query(sql, params),
    run: (sql, ...params) => query(sql, params),
    exec: async statements => { for (const statement of statements) await query(statement); },
    end: () => pool.end(),
  };
}
