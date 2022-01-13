/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.nub.lookup;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("A manual test class to rematch individual datasets")
public class NubMatchServiceTest {

  String[] keys = new String[]{
      "0042d06a-8a95-4eb5-bf84-1453bdec29bb",
      "00b92c50-6efb-48ba-80f8-048b711ac2b9",
      "0189bddc-fc52-4ee3-a359-1e1a9981ad3e",
      "0296b9bb-3646-4732-a305-a37fead2e0f3",
      "02af60c1-b666-4b96-8677-70d2ebc411a4",
      "02d54075-6c21-4d05-b8f8-40828a70b492",
      "02f3688b-c8fb-446b-a79f-ba187f6b2b42",
      "03094c25-9057-4f2e-8f1a-296aadfca17d",
      "032db913-3055-4f7c-83b6-0acd4b0c639f",
      "03cdf81b-341b-41ab-bce4-ad99c6d84c4f",
      "03dc32da-46fe-4ff4-b413-0a878c6f8f9d",
      "03f0054d-cfe8-45e6-80e3-9d88decd12a3",
      "04193c06-8d0e-4af0-8078-71b50f0e53d4",
      "04220044-bd93-49c0-890a-6475ca93fa58",
      "048cd90b-381f-4cce-94a1-846e33bb263e",
      "0498cbab-ba3e-4866-9061-ad026264a358",
      "04b19d7f-1d74-41ec-b657-9b57f47826f9",
      "053bdfd9-33a5-45ca-9412-033fd3cab722",
      "0540f87c-c902-4df5-acd4-51801c9531bd",
      "05454863-d4d2-4de9-b80c-50a41c1f16e7",
      "05b76ae0-dcec-434c-8897-bd71572268c1",
      "05efa1bd-7787-4f5c-a014-e94b9b9f24fa",
      "0657afbe-a3ef-4f09-9361-95b1871b860a",
      "065cd936-3e5f-45f8-ac7f-2d530aab8b4e",
      "067e9fe1-910c-42f8-829f-ded1324ff8b0",
      "06bad515-ea43-452d-befa-d053712dd162",
      "06eb2dc8-dc4c-4db0-934b-4d1d7e0fcded",
      "07abd153-3d09-4820-82dd-6991f1d14ced",
      "07ad5422-fc25-45dd-be81-03cb609ac30e",
      "08056e65-10fb-44e4-a577-2fb27c40cfc5",
      "081f8fb0-0af9-4e5e-8637-0dac4316b719",
      "08352714-1e98-42d5-9f93-e638394549c3",
      "0836c4e7-3b45-4097-8675-e14f3537fb72",
      "096f9dbd-b8f9-491c-b952-8a79357566fd",
      "0a027439-c601-4533-bd03-f0e5d4442801",
      "0a4b5399-02ed-409f-b501-77f52fa2caff",
      "0aa851eb-552a-43c4-b4dd-1dcc0a1297df",
      "0b136600-8384-4130-8c2e-94c6678f3dd2",
      "0b25bedc-a333-4163-b6f5-3f3e2baccbcc",
      "0b63bf08-8d1b-45c3-86c8-b591d108492c",
      "0bef1055-1cca-4a1c-b4cc-50aaa0173a2a",
      "0bf4d7bf-57b7-459c-afa6-5781e3e40b19",
      "0bf987c4-bd60-465c-a4bf-6db70469c184",
      "0c2aa01d-58eb-498b-a278-8b6b4367c1a2",
      "0d005205-2241-4da5-9b00-be4d1a842067",
      "0d0d29eb-b376-4f5d-934d-dd987ff0cbff",
      "0d3127fb-4a68-4a7e-ae88-0e2379df2b11",
      "0d5dbc26-d59b-44d8-aa1e-bc6b29f10b76",
      "0dcda71b-4d8c-4c97-a509-a47f3b2f5573",
      "0df1cd40-d273-4ddb-9fd7-dca7fc392a8b",
  };


  @Test
  public void matchDataset() throws Exception {
//    ClbConfiguration clb = new ClbConfiguration();
//    clb.serverName = "backbonebuild-vh.gbif.org";
//    clb.databaseName = "clb";
//    clb.user = "clb";
//    clb.password = "";
//
//    NeoConfiguration neo = new NeoConfiguration();
//    neo.neoRepository = new File(FileUtils.getTempDirectory(), "clb");
//
//    Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(clb));
//    DatasetImportService sqlService = inj.getInstance(Key.get(DatasetImportService.class, Mybatis.class));
//
//    //IdLookup lookup = IdLookupImpl.temp().load(clb, false);
//    IdLookup lookup = IdLookupImpl.temp();
//    NubMatchService service = new NubMatchService(clb, neo, lookup, null, null);
//    for (String key : keys) {
//      service.matchDataset(UUID.fromString(key));
//    }
//    // IUCN 19491596-35ae-4a91-9a98-85cf505f1bd3
//    service.matchDataset(UUID.fromString("19491596-35ae-4a91-9a98-85cf505f1bd3"));
  }

}
