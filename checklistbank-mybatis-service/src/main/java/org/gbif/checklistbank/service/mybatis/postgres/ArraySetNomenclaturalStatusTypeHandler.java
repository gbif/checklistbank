package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.api.vocabulary.NomenclaturalStatus;

public class ArraySetNomenclaturalStatusTypeHandler extends ArraySetTypeHandler<NomenclaturalStatus> {

  public ArraySetNomenclaturalStatusTypeHandler() {
    super("nomenclatural_status");
  }

  @Override
  protected NomenclaturalStatus convert(String x) {
    return NomenclaturalStatus.fromString(x);
  }
}
