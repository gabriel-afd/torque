import { PaginadorIntl } from './paginador-intl';

describe('PaginadorIntl', () => {
  const intl = new PaginadorIntl();

  it('deve mostrar os textos do paginador em português', () => {
    expect(intl.itemsPerPageLabel).toBe('Itens por página');
    expect(intl.getRangeLabel(0, 10, 10)).toBe('1–10 de 10');
    expect(intl.getRangeLabel(1, 20, 37)).toBe('21–37 de 37');
    expect(intl.getRangeLabel(0, 20, 0)).toBe('0 de 0');
  });
});
