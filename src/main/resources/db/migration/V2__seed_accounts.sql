-- Conta interna de liquidacao: contrapartida de depositos e saques (dinheiro que entra e sai do banco).
INSERT INTO accounts (id, holder_name, kind, balance)
VALUES (0, 'Conta de Liquidacao', 'SETTLEMENT', 0);

-- 10.000 contas de clientes usadas pelos testes de carga.
INSERT INTO accounts (id, holder_name, kind, balance)
SELECT n, 'Cliente ' || n, 'CUSTOMER', 1000000.00
FROM generate_series(1, 10000) AS n;
