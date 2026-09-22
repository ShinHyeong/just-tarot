from __future__ import annotations

import difflib
import json
import os
import re
import pathlib
import sys
import time

from dotenv import load_dotenv
load_dotenv()

from openai import OpenAI

MODEL = os.environ.get("OPENAI_MODEL", "gpt-6-astra")
OUT_DIR = pathlib.Path("db/seeds/cards")
SQL_PATH = pathlib.Path("v2__seed_cards.sql")
REVIEW_PATH = pathlib.Path("db/seeds/axis_review.json")

# --------------------------------------------------------------
# 1. 카드 78장 목록만 생성
# --------------------------------------------------------------

MAJOR_NAMES = [
  "The Fool", "The Magician", "The High Priestess", "The Empress",
  "The Emperor", "The Hierophant", "The Lovers", "The Chariot",
  "Strength", "The Hermit", "Wheel of Fortune", "Justice",
  "The Hanged Man", "Death", "Temperance", "The Devil",
  "The Tower", "The Star", "The Moon", "The Sun",
  "Judgement", "The World",
]

SUITS = ["WANDS", "CUPS", "SWORDS", "PENTACLES"]

SUIT_ELEMENT = {
    "WANDS": "의지, 행동",
    "CUPS": "감정, 관계",
    "SWORDS": "사고, 갈등",
    "PENTACLES": "현실, 물질",
}

RANKS = {
 1: "Ace", 2: "Two", 3: "Three", 4: "Four", 5: "Five",
 6: "Six", 7: "Seven", 8: "Eight", 9: "Nine", 10: "Ten",
 11: "Page", 12: "Knight", 13: "Queen", 14: "King",
}

MAJOR_NAMES_KO = [
    "바보", "마법사", "여사제", "여황제", "황제", "교황", "연인", "전차",
    "힘", "은둔자", "운명의 수레바퀴", "정의", "매달린 사람", "죽음", "절제", "악마",
    "탑", "별", "달", "태양", "심판", "세계",
]
SUIT_KO = {"WANDS": "완드", "CUPS": "컵", "SWORDS": "소드", "PENTACLES": "펜타클"}
RANK_KO = {1: "에이스", 11: "페이지", 12: "나이트", 13: "퀸", 14: "킹"}

def build_cards() -> list[dict]:
    cards: list[dict] = []
    id = 1
    for number, name in enumerate(MAJOR_NAMES):
         cards.append({
            "id": id,
            "name": name,
            "name_ko": MAJOR_NAMES_KO[number],
            "arcana": "MAJOR",
            "suit": "NONE",
            "card_number": number,
         })
         id += 1
    for suit in SUITS:
        for number in range(1, 15):
            cards.append({
                "id": id,
                "name": f"{RANKS[number]} of {suit.title()}",
                "name_ko": f"{SUIT_KO[suit]} {RANK_KO.get(number, number)}",
                "arcana": "MINOR",
                "suit": suit,
                "card_number": number,
            })
            id += 1
    return cards


# --------------------------------------------------------------
# 2. OpenAI API에게 받아올 결과값 설정 (상징 등)
# --------------------------------------------------------------

SCHEMA = {
           "name": "card_info",
           "strict": True,
           "schema": {
             "type": "object",
             "properties": {
               "symbols": {
                 "type": "array",
                 "minItems": 4,
                 "maxItems": 5,
                 "items": {
                   "type": "object",
                   "properties": {
                     "symbol": {
                       "type": "string",
                       "description": "카드 그림에 실제로 그려진 시각요소. <visuals> 안 <item> 의 표현을 그대로 쓴다. 수식을 덜어낼 때도 그 항목의 연속된 일부여야 한다. 15자 이내 명사구. 추상 개념을 넣지 않는다."
                     },
                     "meaning": {
                       "type": "string",
                       "description": "그 요소가 갖는 의미. 25자 이내. 길흉 판단이나 조언이 아니라 상태 기술만 한다."
                     }
                   },
                   "required": [
                     "symbol",
                     "meaning"
                   ],
                   "additionalProperties": False
                 }
               },
               "themes": {
                 "type": "array",
                 "minItems": 3,
                 "maxItems": 5,
                 "items": {
                   "type": "string",
                   "description": "한 단어~두 단어의 추상 명사. 문장으로 쓰지 않는다."
                 }
               },
               "uprightEnergy": {
                 "type": "string",
                 "description": "정방향으로 놓였을 때의 상태. 30자 이내 한 문장."
               },
               "reversedEnergy": {
                 "type": "string",
                 "description": "역방향으로 놓였을 때의 상태. 30자 이내 한 문장. 정방향의 단순 반대가 아니라 같은 에너지가 막히거나 정체되거나 안으로 향하거나 과잉으로 흐르는 상태로 쓴다. 단순 부정문을 쓰지 않는다."
               },
               "axisSupport": {
                 "type": "object",
                 "description": "axis 를 명명하기 전에, 어떤 symbol 이 어느 항을 받치는지 먼저 확정한다. 각 항목은 symbols 의 symbol 문자열과 정확히 일치해야 한다.",
                 "properties": {
                   "a": {
                     "type": "array",
                     "minItems": 1,
                     "maxItems": 4,
                     "items": {
                       "type": "string",
                       "description": "A 항을 뒷받침하는 symbol. 해당 symbol 의 meaning 이 실제로 A 쪽으로 기울어야 한다."
                     }
                   },
                   "b": {
                     "type": "array",
                     "minItems": 1,
                     "maxItems": 4,
                     "items": {
                       "type": "string",
                       "description": "B 항을 뒷받침하는 symbol. a 와 겹치는 항목이 있으면 안 된다. 같은 symbol 을 양쪽에 넣어 균형을 맞추지 마라."
                     }
                   }
                 },
                 "required": [
                   "a",
                   "b"
                 ],
                 "additionalProperties": False
               },
               "axis": {
                 "type": "string",
                 "description": "axisSupport 로 확보한 두 항을 'A vs B' 형태로 명명한다. 카드가 둘 중 한쪽이라는 뜻이 아니라 이 축 위에서 흔들린다는 뜻이다. 내부 갈등이 뚜렷하지 않은 카드는 '이 카드가 주는 것 vs 그것이 치르는 대가'를 축으로 삼는다."
               }
             },
             "required": [
               "symbols",
               "themes",
               "uprightEnergy",
               "reversedEnergy",
               "axisSupport",
               "axis"
             ],
             "additionalProperties": False
           }
         }

SYSTEM_PROMPT = """\
당신은 라이더-웨이트(Rider-Waite) 덱을 기준으로 타로 카드의 상징 체계를 정리하는 자료 편집자다.
당신이 만드는 출력물은 완성된 해설문이 아니다. 이후 다른 카드 2~3장과 조합되어
하나의 서사로 재구성될 '원자 단위 재료'다. 따라서 짧고, 중립적이고, 재사용 가능해야 한다.
규칙:
- 카드 이름을 제외한 모든 값은 한국어로 쓴다.
- symbols: symbol 은 그림 속 사물이나 인물을 가리키는 짧은 명사구다. 추상 개념을 symbol 자리에 넣지 않는다.
  symbol 은 반드시 <visuals> 안의 <item> 에서만 고른다. 거기 없는 요소를 지어내지 않는다.
  수식은 그 요소를 특정하는 데 꼭 필요한 것만 남긴다.
	(O: "떨어지는 왕관", "만족한 표정의 승자", "쏟아진 세 컵")
	(X: "검을 들고 만족스러운 표정을 짓는 체격이 큰 남자")
	해석이나 상태 설명은 symbol 이 아니라 meaning 에 쓴다.
	길이는 대체로 15자 안쪽이 된다. 크게 넘으면 수식을 덜어낸다.
	symbol 하나에 두 요소를 합치지 않는다. (X: "돌다리와 성")
  meaning 은 목록에 없으므로 규칙에 맞춰 직접 쓴다.
- symbols 선택 규칙: axis 의 A 와 B 를 각각 뒷받침하는 요소를 최소 하나씩 포함한다.
  한쪽 항의 근거가 목록에 없다면 axis 를 다시 잡는다.
  A 와 B 의 근거 개수가 한쪽으로 크게 치우치지 않게 한다.
  약한 쪽 항의 근거를 먼저 확보한 뒤 나머지를 채운다.

- themes: 한 단어~두 단어의 추상 명사. 문장으로 쓰지 않는다.
- uprightEnergy: 카드가 정방향으로 놓였을 때의 상태. 30자 이내 한 문장.
- reversedEnergy: 카드가 역방향으로 놓였을 때의 상태. 30자 이내 한 문장.
  역방향은 정방향의 '반대'가 아니다. 같은 에너지가 막히거나, 정체되거나,
  안으로 향하거나, 과잉으로 흐르는 상태로 쓴다.
  위에 제시한 표현("안으로 파고듦", "느슨해지지만 아직") 자체를 그대로 쓰지 마라. 카드 고유의 어휘로 같은 구조를 다시 써라.
  단순 부정문("~하지 않음", "~가 없음", "~의 부재")으로 쓰지 않는다.
  정방향이 이미 상실·정체·붕괴를 뜻하는 카드(The Tower, Ten of Swords, Five of Pentacles 등)는
  역방향을 '그 상태가 느슨해지기 시작하지만 아직 끝나지 않음' 또는 '그 상태가 안으로 파고듦'으로 쓴다.
  단순히 "회복됨", "좋아짐"으로 뒤집지 않는다.
- axis: 이 카드가 놓여 있는 대립 '축'을 "A vs B" 형태로 쓴다.
  카드가 둘 중 한쪽이라는 뜻이 아니라, 이 카드가 그 축 위에서 흔들린다는 뜻이다.
  내부 갈등이 뚜렷하지 않은 카드는 '이 카드가 주는 것 vs 그것이 치르는 대가'를 축으로 삼는다.
  (예: The Sun → "드러냄 vs 가려질 곳이 없음")
  - 인물의 의복 색만으로 축의 양항을 세우지 마라. 행위·자세·시선을 먼저 고른다.
  - 마이너 숫자 카드(2~10): 숫자의 뜻과 슈트의 영역을 결합해서 쓴다.
  같은 숫자의 다른 슈트 카드와 themes 가 겹치지 않도록, 슈트의 영역이 드러나는 어휘를 고른다.
- 궁정 카드(Page/Knight/Queen/King): 인물의 자세, 시선, 발밑과 주변 사물을 symbol 로 삼는다.
  "왕좌", "망토"처럼 16장에 다 있는 요소는 피한다. themes 에는 그 인물의 성숙 단계가 드러나야 한다.
- 길흉 판단, 예언, 조언 문구를 쓰지 않는다. 오직 상태 기술만 한다.
- 다른 카드와 어휘 수준이 균일해야 한다. 아래 두 예시의 추상화 수준을 그대로 따른다.
[예시 1 — Wheel of Fortune]
{
  "symbols": [
    {"symbol": "돌아가는 바퀴", "meaning": "순환, 통제 밖의 흐름"},
    {"symbol": "네 모서리의 생물", "meaning": "변하지 않는 고정점"},
    {"symbol": "스핑크스", "meaning": "변화 위의 수수께끼, 균형"}
  ],
  "themes": ["순환", "전환점", "타이밍", "외부 요인"],
  "uprightEnergy": "흐름이 움직이기 시작함",
  "reversedEnergy": "흐름이 꼬이거나 같은 패턴 반복",
  "axis": "인간의 의지 vs 통제할 수 없는 흐름"
}
[예시 2 — Five of Pentacles]
{
  "symbols": [
    {"symbol": "눈 속의 두 사람", "meaning": "결핍 속에서도 이어지는 동행"},
    {"symbol": "불 켜진 창문", "meaning": "가까이 있지만 닿지 않는 도움"},
    {"symbol": "목발", "meaning": "누적된 손상, 느린 걸음"}
  ],
  "themes": ["결핍", "소외", "버팀", "외면"],
  "uprightEnergy": "필요한 것이 곁에 있어도 닿지 못함",
  "reversedEnergy": "결핍이 느슨해지지만 아직 문 앞에 서 있음",
  "axis": "홀로 버팀 vs 도움을 청함"
}
"""

NUMBER_HINT = {
    1: "시작, 순수한 잠재력",
    2: "이원성, 선택, 균형",
    3: "확장, 협력, 첫 결실",
    4: "안정, 구조, 정체",
    5: "갈등, 상실, 흔들림",
    6: "조화, 회복, 교환",
    7: "평가, 시험, 인내",
    8: "움직임, 숙련, 힘의 이동",
    9: "성숙, 거의 완성, 홀로 감당",
    10: "완결, 과잉, 순환의 끝",
    11: "Page — 탐색하는 초심자, 소식",
    12: "Knight — 행동으로 밀어붙이는 추진자",
    13: "Queen — 내면화하고 품는 성숙",
    14: "King — 통제하고 책임지는 완성",
}

# 시각 묘사 정확도를 위해 컨텍스트 제공
# 그림에 실제로 그려진 시각 요소만. 해석은 쓰지 않는다.(LLM의 몫이기 때문에)
CARD_VISUALS: dict[str, list[str]] = {
    "The Fool": ["절벽 끝에 선 인물", "위를 향한 시선", "화려한 무늬의 옷", "얇은 옷", "손에 든 흰 장미", "막대 끝에 매단 작은 보따리", "발치의 흰 개", "뒤편의 설산", "하얀 태양", "하얀 속옷", "노란 하늘"],
    "The Magician": ["하늘로 든 손", "머리띠", "빨간색 옷", "흰 옷", "우로보로스 허리띠", "탁자 위 4가지 도구", "장미와 백합", "무한대 기호", "위에서 아래로 가리키는 포즈"],
    "The High Priestess": ["보아즈(검은색)", "야킨(흰색)", "석류 무늬 천막", "토라 경전", "흐르는 물처럼 보이는 옷자락", "이시스의 왕관", "폐쇄적인 자세", "굳게 다문 입", "바다", "달 모형"],
    "The Empress": ["홀", "빨갛고 부드러운 쿠션", "빨간 쿠션에 편안히 기대고 있음", "석류 무늬 드레스", "비너스의 방패", "곡식 밭", "숫자 3", "울창한 곡식·숲/강·폭포", "돌바닥"],
    "The Emperor": ["위가 닫힌 돔 형태의 왕관", "앙크 십자가", "돌로 된 산맥", "붉은 로브", "갑옷", "숫양의 머리", "황제의 시선", "십자가 없는 보주", "얇게 흐르는 강물", "돌로 된 왕좌"],
    "The Hierophant": ["회색기둥", "3단 티아라", "스태프", "교황", "하늘을 가리키는 손", "빨간 옷", "십자가 3개", "천국의 열쇠", "주케토(모자)", "두 사제", "장미 옷을 입은 사제", "백합 옷을 입은 사제"],
    "The Lovers": ["대천사 라파엘", "지식의 나무", "아무 옷도 안 걸친 두 사람", "산", "생명의 나무", "남녀의 시선", "아담과 이브"],
    "The Chariot": ["파란색 별무늬 천막", "월계관", "지휘봉", "사각형 문양", "배경에 보이는 성", "서 있는 모습", "파란색 갑옷", "돌로 만든 전차", "흑백의 스핑크스", "팽이 모양의 문장", "노란 배경"],
    "Strength": ["무한대 기호", "노란 배경", "하얀 옷", "꽃으로 된 벨트", "붉은 사자", "산", "꼬리가 안으로 말려있음", "아래를 향한 여자 포즈", "위를 향한 사자 포즈"],
    "The Hermit": ["등불을 들은 오른손", "후드가 있는 망토", "지팡이를 든 왼손", "흰색 수염", "고개를 떨구고 있음", "회색 망토", "등불 속 육각형 별", "눈"],
    "Wheel of Fortune": ["검을 든 스핑크스", "히브리어(Y, H, V, H)", "이집트 신 세트 또는 티폰(뱀)", "수레바퀴", "네 모서리의 생물", "책", "T·A·R·O 글자", "늑대", "구름"],
    "Justice": ["똑바로 서있는 양날의 검", "회색 기둥", "보라색 천막", "눈가리개가 없음", "사각형 보석이 박힌 왕관", "붉은 원이 그려진 사각형 브로치", "검을 쥔 손의 모양", "양팔 저울", "붉은 로브", "녹색 망토", "하얀 신발"],
    "The Hanged Man": ["잎사귀", "묶여있는 한쪽 발", "T자형 나무", "빨간 바지", "파란 옷", "구부린 다리", "등 뒤에 놓은 손", "평온한 표정", "후광", "거꾸로 매달림"],
    "Death": ["검은 깃발", "하얀 장미", "2개의 탑", "태양", "바닥에 있는 사람들", "강", "배", "흰색 말", "갑옷", "해골 기사", "빨간 깃털"],
    "Temperance": ["이마의 태양 마크", "대천사 미카엘", "정사각형 안의 삼각형", "왕관", "컵의 물을 조절하는 행위", "붓꽃", "한 발은 연못에 한 발은 땅에", "잔잔한 수면", "길"],
    "The Devil": ["오른손의 모양", "악마 바포메트", "쇠사슬", "역오망성", "악마의 당나귀 귀", "거꾸로 든 횃불", "인간의 꼬리", "반쪽짜리 직육면체 왕좌"],
    "The Tower": ["번개를 맞은 탑", "떨어지는 왕관", "떨어지는 사람들", "탑을 감싼 불길", "공중에 흩어진 22개의 불꽃", "좁고 험한 바위산 꼭대기", "회색 하늘(회색 연기)"],
    "The Star": ["7개의 작은 별들", "여인의 시선", "두 개의 물병", "물웅덩이", "별", "새", "발가벗은 여인", "무릎의 위치", "작은 새싹"],
    "The Moon": ["달 속의 얼굴", "두 개의 탑", "개", "길", "가재", "달", "15개의 요드 기호", "늑대", "개와 늑대가 동시에 짖음"],
    "The Sun": ["푸른 색의 밝은 하늘", "직선/곡선 햇빛", "태양", "붉은 깃털", "해바라기", "붉은 깃발", "벌거벗은 아이", "흰 말", "담장"],
    "Judgement": ["푸른 색의 맑은 하늘", "빨간색 등변 십자가", "눈 덮인 산", "관이 떠 있는 바다", "돌로 만들어진 관", "대천사 가브리엘", "나팔", "7개의 기본음", "일어나는 사람들", "뻗은 두 팔"],
    "The World": ["네 개의 생물(사자·소·독수리·사람)", "보라색 천", "춤추는 나체의 인물", "구름", "월계수 화환(타원형)", "양손의 지팡이", "붉은색 매듭"],

    "Ace of Wands": ["깨끗한 하늘", "피어나는 새싹", "완드를 잡은 손", "멀리 보이는 성", "초목이 우거진 배경", "완드", "떨어지는 잎사귀", "구름 속에서 나온 손"],
    "Two of Wands": ["땅과 바다", "지구본", "남자가 쥐고 있는 완드", "장미와 백합 십자가", "남자의 시선", "성벽에 묶여있는 완드", "붉은 모자와 망토", "멀리 보이는 산"],
    "Three of Wands": ["황금빛 일몰", "머리띠", "초록색 어깨망토", "빨간 망토", "땅에 박힌 세 개의 완드", "바다 위의 배", "완드 하나에 살짝 기대어 있음", "남자가 등을 돌린 채 먼 바다를 바라봄"],
    "Four of Wands": ["노란 하늘", "화환", "배경의 성", "꽃다발을 든 두 사람", "배경의 사람들", "네 개의 완드", "다리"],
    "Five of Wands": ["파란 하늘", "완드가 무질서하게 엉켜있음", "남자들의 옷차림", "척박한 땅", "공격하는 사람", "방어하는 사람", "서로 눈을 마주치지 않음"],
    "Six of Wands": ["완드들이 평행하게 서있음", "월계관", "높이 치켜든 완드", "회색(또는 흰색) 말", "초록색 천", "구름 없는 푸른 하늘", "붉은 망토", "뻣뻣한 자세(턱을 추켜들고)"],
    "Seven of Wands": ["파란색 하늘", "손에 쥐어진 완드 하나", "험준한 언덕", "짝짝이 신발", "아래에서 올라오는 6개의 완드", "초록색 옷", "완드를 쥔 손모양"],
    "Eight of Wands": ["맑고 푸른 하늘", "빠르게 날아가는 완드 8개", "흐르는 강", "사람의 부재"],
    "Nine of Wands": ["파란색 하늘", "8개의 완드", "붕대", "남자의 표정", "단단하고 안정적인 땅", "녹색 배경", "상처 입은 남자"],
    "Ten of Wands": ["파란색 하늘", "10개의 완드", "짐을 진 남자", "마을", "단단하고 안정적인 땅", "나무와 들판"],
    "Page of Wands": ["깃털이 달린 모자", "흰색 모자", "붉은 깃털", "완드를 바라보는 시선", "살라마드(불도마뱀 무늬의 옷)", "피라미드", "메마른 사막"],
    "Knight of Wands": ["붉은 깃털", "갑옷", "살라마드(불도마뱀 무늬의 옷)", "앞발을 든 붉은 말", "피라미드", "메마른 사막"],
    "Queen of Wands": ["나뭇잎 장식 왕관", "해바라기", "돌로 된 바닥", "검은 고양이", "돌 내부에 있는 완드", "사자 장식이 있는 왕좌"],
    "King of Wands": ["꽃이 피어나는 완드", "사자", "살라마드(불도마뱀 무늬)", "녹색 망토·신발", "붉은 옷", "돌 외부에 있는 완드", "돌로 된 바닥", "홀로 있는 도마뱀"],

    "Ace of Cups": ["성체(wafer)를 물고 있는 비둘기", "다섯 갈래의 물줄기", "황금 성배", "구름 속에서 나온 손", "성배를 받치는 손", "떨어지는 물방울", "고요한 물", "수련"],
    "Two of Cups": ["사자 머리", "헤르메스의 지팡이(카두세우스)", "남자가 쓴 빨간색 화관", "여자가 쓴 초록색 월계관", "컵을 교환하는 모습", "여자의 옷차림(파란색, 흰색)", "남자의 옷차림(노란색, 붉은색)", "배경의 집", "한 발을 앞으로 내딤"],
    "Three of Cups": ["높이 든 컵", "꽃 화환", "춤추는 세 명의 여성", "흩날리는 옷자락", "과일과 채소", "원형 대형"],
    "Four of Cups": ["나무", "맑은 하늘", "남자의 시선", "구름 속 손이 컵을 건네고 있음", "팔과 다리를 꼬고 있음", "세 개의 컵"],
    "Five of Cups": ["흐린 하늘", "검은 망토", "망토를 두른 인물", "숙인 고개", "쏟아진 컵을 바라보는 인물", "왼쪽을 향해있는 시선", "쏟아진 세 개의 컵", "액체가 쏟아져 있음", "똑바로 서 있는 두 개의 컵", "거세게 흐르는 강", "강 위의 돌다리", "건너편의 성"],
    "Six of Cups": ["맑고 푸른 하늘", "건물", "돌바닥", "무기를 든 성인 남자", "X가 새겨진 돌기둥", "하얀 꽃이 담겨있는 컵", "두 아이"],
    "Seven of Cups": ["뱀", "장막에 가려진 인물", "사람의 머리", "용", "성(또는 타워)", "해골 그림자가 있는 화환", "보석", "구름", "중심 인물"],
    "Eight of Cups": ["짙고 푸른 하늘", "태양과 달이 섞인 모습", "산", "망토를 두른 인물", "강", "8개의 컵", "척박한 땅"],
    "Nine of Cups": ["노란색 배경", "9개의 컵", "빨간색 모자", "푸른색 커튼", "앉아있는 인물", "나무 벤치", "헐렁한 하얀색 옷"],
    "Ten of Cups": ["무지개", "소박한 집", "부부", "춤추는 아이들", "견고한 땅"],
    "Page of Cups": ["베레모와 스카프", "컵을 바라보는 시선", "컵을 든 시종", "컵 속의 물고기", "수련 무늬가 있는 파란색 튜닉", "바다의 물결", "신발"],
    "Knight of Cups": ["날개 달린 투구와 신발", "갑옷", "빨간 물고기", "한 손으로는 고비를 잡아 말을 이끌고 다른 한 손으로는 컵을 내밀고 있는 모습", "회색(또는 흰색) 말", "매우 천천히 우아하게 걷고 있는 모습", "고개를 숙이고 있는 모습", "산", "강"],
    "Queen of Cups": ["진주로 장식된 왕관", "바다의 요정", "뚜껑이 닫힌 컵", "천사 및 십자가 장식", "화려한 컵", "꼿꼿한 자세로 컵을 응시하고 있는 모습", "하얀(푸른) 옷", "돌 왕좌", "조약돌", "잔잔하게 소용돌이치는 바다"],
    "King of Cups": ["오른손 중지에 반지", "금색과 붉은색이 섞인 왕관", "지휘봉", "초록색 망토", "붉은 배", "돌고래 또는 물고기", "파란색 로브", "돌 왕좌", "돌바닥", "바다 위에 있음"],

    "Ace of Swords": ["월계수 장식이 있는 왕관", "꼿꼿이 선 소드", "황금빛 불꽃", "검을 꽉 쥐고 있는 손", "하얀 섬광", "산악 지형"],
    "Two of Swords": ["초승달", "두 개의 검을 X자로 교차함", "눈을 가린 여성", "바위", "바다", "회색(흰색) 로브", "회색 벤치", "안정적인 땅"],
    "Three of Swords": ["폭풍우", "심장을 관통", "세 개의 검"],
    "Four of Swords": ["스테인드글라스 창문", "벽에 걸려 있는 검들", "누워 있는 기사의 석상", "관 밑에 누워 있는 검"],
    "Five of Swords": ["요동치는 하늘", "바람이 휘몰아치고 있는 머리카락", "만족한 표정의 승자", "세 검을 든 손", "체격이 큰 승자", "바닥에 놓인 검", "등을 돌리고 멀어지는 남자", "울고 있는 남자"],
    "Six of Swords": ["저 멀리 보이는 기슭", "배에 꽂혀 있는 검", "뱃사공이 배를 이끌고 이동함", "승객", "잔잔한 물", "물결이 이는 물"],
    "Seven of Swords": ["노란색 하늘", "뒤를 돌아보는 모습", "남자가 검을 훔쳐 달아나고 있음", "남겨진 2개의 검", "남자의 발끝"],
    "Eight of Swords": ["잿빛 하늘", "눈이 가려짐", "여자를 둘러싼 8개의 검", "느슨한 결박", "붉은 드레스", "물이 고여있는 땅"],
    "Nine of Swords": ["해가 떠오르고 있음", "잔잔한 바다", "붉은 망토", "검에 찔린 채 납작 엎드린 남자", "신의 축복을 구하는 손짓"],
    "Ten of Swords": ["머리 위에 떠있는 검 9개", "어두운 배경", "괴로워하는 사람", "이불", "침대에 조각된 그림 (전투 장면)"],
    "Page of Swords": ["시종의 시선", "거센 바람", "검을 쥐고 있는 모습", "구름", "발놀림 (또는 빠른 걸음)", "울퉁불퉁한 언덕"],
    "Knight of Swords": ["검을 앞으로 치켜듦", "붉은 망토", "소드 기사", "갑옷", "앞만 보고 빠르게 돌진하는 모습"],
    "Queen of Swords": ["똑바로 선 검", "새 한 마리", "나비 왕관", "여왕의 포즈", "소드 여왕", "구름", "왕좌"],
    "King of Swords": ["왕좌", "왕관", "정면을 응시하고 있음", "검을 뽑아 들음", "소드 왕", "푸른색 가운", "붉은 망토", "작은 회색 구름", "나무"],

    "Ace of Pentacles": ["구름 속에서 나온 손", "황금색 펜타클", "아치형 문", "붉은 장미", "흰 백합", "산"],
    "Two of Pentacles": ["높고 붉은 모자", "녹색 띠", "펜타클을 저글링 하는 행위", "화려한 옷차림", "단단한 회색 땅", "배가 떠 있는 거친 바다"],
    "Three of Pentacles": ["성당에 새겨진 세 개의 펜타클", "서로 다른 복장", "망치를 든 장인", "수도사와 귀족의 모습", "나무 벤치", "어두운 배경"],
    "Four of Pentacles": ["펜타클의 위치(가슴 앞, 머리 위, 발 밑)", "회색 하늘", "어딘가 초라한 왕관", "검은 망토", "인물의 자세", "인물의 표정", "옷", "배경의 도시"],
    "Five of Pentacles": ["창문 안의 펜타클", "밝게 빛나는 스테인드 글라스", "목에 걸린 종", "두 인물", "눈 덮인 바닥"],
    "Six of Pentacles": ["상인의 옷", "양팔 저울", "동전을 건네는 모습", "성", "빨간 티켓"],
    "Seven of Pentacles": ["남자의 자세", "남자의 표정", "여섯 개의 펜타클이 달린 덤불", "떨어져 있는 펜타클", "짝짝이 신발"],
    "Eight of Pentacles": ["기둥에 걸린 펜타클", "망치와 끌", "남자의 표정", "푸른색 옷", "붉은색 바지", "남자가 펜타클을 조각하는 모습"],
    "Nine of Pentacles": ["노란 하늘", "산", "포도", "여자를 둘러싼 펜타클", "달팽이", "매", "울타리가 쳐진 정원", "꽃무늬 옷"],
    "Ten of Pentacles": ["성 모양의 문장", "가문의 창립자 노인", "열 개의 펜타클", "배경의 도시", "일상적인 모습", "두 마리의 개"],
    "Page of Pentacles": ["붉은색 모자", "노란 하늘", "나무", "꽃과 풀들", "시종의 시선", "시종의 포즈", "펜타클 시종", "녹색 튜닉", "밭"],
    "Knight of Pentacles": ["나뭇잎이 달린 투구", "붉은 색 굴레/옷/고삐", "펜타클 기사", "갈아 놓은 밭", "흑마가 제자리에 서있음", "흑마", "기사의 포즈"],
    "Queen of Pentacles": ["장미", "펜타클 여왕", "여왕의 포즈", "산", "염소", "강", "토끼", "정원"],
    "King of Pentacles": ["황소가 새겨진 왕좌", "펜타클 왕", "왕이 가진 펜타클", "지휘봉", "포도 장식의 옷", "황소 머리 발받침대", "갑옷", "성", "꽃으로 장식된 왕관", "왕의 표정"],
}

def esc_xml(text: str) -> str:
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def user_prompt(card: dict) -> str:
    """카드 1장의 입력을 XML 로 만든다.

    시각 요소는 항목 경계가 모호해지면 안 되므로 <item> 하나에 하나씩 넣는다.
    쉼표로 이어 붙이면 "펜타클의 위치(가슴 앞, 머리 위, 발 밑)" 같은 항목에서
    모델도 검증기도 경계를 잘못 잡는다.
    """
    meta = [f"  <name>{esc_xml(card['name'])}</name>"]
    if card["arcana"] == "MAJOR":
        meta.append("  <arcana>메이저 아르카나</arcana>")
        meta.append(f"  <number>{card['card_number']}</number>")
    else:
        meta.append("  <arcana>마이너 아르카나</arcana>")
        meta.append(
            f"  <suit domain=\"{SUIT_ELEMENT[card['suit']]}\">{card['suit']}</suit>"
        )
        meta.append(
            f"  <rank number=\"{card['card_number']}\">{RANKS[card['card_number']]}</rank>"
        )
        meta.append(
            f"  <rank_meaning>{esc_xml(NUMBER_HINT[card['card_number']])}</rank_meaning>"
        )

    parts = ["<card>", *meta, "</card>"]

    visuals = CARD_VISUALS.get(card["name"])
    if visuals:
        parts.append("")
        parts.append("<visuals>")
        parts += [f"  <item>{esc_xml(v)}</item>" for v in visuals]
        parts.append("</visuals>")

    parts.append("")
    parts.append(
        "<task>이 카드의 상징, 의미, 긴장 구조를 규칙에 맞춰 JSON으로 정리하라. "
        "symbol 은 <visuals> 안의 <item> 에서만 고른다.</task>"
    )
    return "\n".join(parts)


# --------------------------------------------------------------
# 3. API 호출
# --------------------------------------------------------------

def out_path(card: dict) -> pathlib.Path:
    return OUT_DIR / f"{card['id']:02d}_{card['arcana']}_{card['suit']}_{card['card_number']:02d}.json"

EFFORTS = ["high", "medium"]   # 1차는 high, 검증 실패 시 medium 으로 재시도

def call_model(client: OpenAI, card: dict, effort: str) -> dict | None:
    """단일 카드 1회 호출. 네트워크/파싱 실패만 여기서 재시도한다."""
    for attempt in range(1, 4):
        try:
            res = client.responses.create(
                model=MODEL,
                instructions=SYSTEM_PROMPT,
                input=user_prompt(card),
                reasoning={"effort": effort},
                max_output_tokens=6000,
                text={
                    "verbosity": "low",
                    "format": {
                        "type": "json_schema",
                        "name": SCHEMA["name"],
                        "strict": SCHEMA["strict"],
                        "schema": SCHEMA["schema"],
                    },
                },
                store=True,
            )

            if res.status == "incomplete":
                raise RuntimeError(f"출력 상한 초과: {res.incomplete_details.reason}")

            raw = res.output_text
            if not raw:
                raise RuntimeError("빈 응답: refusal 가능성, 응답 확인 필요")

            print(f"  usage: in={res.usage.input_tokens} "
                  f"(cached={res.usage.input_tokens_details.cached_tokens}) "
                  f"out={res.usage.output_tokens} "
                  f"reasoning={res.usage.output_tokens_details.reasoning_tokens}")

            return json.loads(raw)
        except Exception as e:
            print(f"  retry {attempt}/3 ({card['name']}, {effort}): {e}")
            time.sleep(2 * attempt)
    return None


def generate() -> None:
    client = OpenAI(timeout=180.0, max_retries=0)
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    for card in build_cards():
        path = out_path(card)
        if path.exists():
            print(f"skip  {card['name']}")
            continue

        print(f"→ {card['name']} 요청 중...", flush=True)

        chosen, chosen_effort, chosen_problems = None, None, None
        for effort in EFFORTS:
            content = call_model(client, card, effort)
            if content is None:
                continue
            problems = validate(card, content)
            if chosen is None or len(problems) < len(chosen_problems):
                chosen, chosen_effort, chosen_problems = content, effort, problems
            if not problems:
                break
            print(f"  검증 실패 ({card['name']}, {effort}): " + " / ".join(problems))

        if chosen is None:
            print(f"FAIL  {card['name']} — 나중에 다시 실행하세요")
            continue

        record = {**card, **chosen, "_model": MODEL, "_effort": chosen_effort}
        path.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding="utf-8")
        status = "ok   " if not chosen_problems else "WARN "
        print(f"{status} {card['name']} ({chosen_effort})")


# --------------------------------------------------------------
# 4. 호출한 값 검증
# --------------------------------------------------------------

def load_all() -> list[dict]:
    records = []
    for card in build_cards():
        path = out_path(card)
        if not path.exists():
            print(f"[누락] {card['name']} ({path})")
            continue
        records.append(json.loads(path.read_text(encoding="utf-8")))
    return records

# 내부 갈등이 뚜렷하지 않아 tension 이 상투적으로 채워지기 쉬운 카드들.
# 자동 판별 불가능하므로 직접 검수한다.
AXIS_REVIEW = {
    "The Sun", "The Star", "The World", "The Empress", "Temperance",
    "Ace of Wands", "Ace of Cups", "Ace of Swords", "Ace of Pentacles",
    "Four of Wands", "Six of Wands", "Three of Cups", "Nine of Cups",
    "Ten of Cups", "Ten of Pentacles", "Eight of Pentacles",
    # 시스템 프롬프트에 예시로 박혀 있어 그대로 베낄 위험이 있는 카드
    "Wheel of Fortune", "Five of Pentacles",
}

# SYSTEM_PROMPT 에 예시로 들어 있는 문자열. 그대로 재생산하면 안 된다.
EXAMPLE_AXES = (
    "인간의 의지 vs 통제할 수 없는 흐름",
    "홀로 버팀 vs 도움을 청함",
    "드러냄 vs 가려질 곳이 없음",
)

# 정방향이 이미 붕괴·상실인 카드를 "회복"으로 뒤집는 것을 막는다.
RECOVERY_WORDS = ("회복", "좋아짐", "나아짐", "치유", "정리됨", "해소됨", "안정을 되찾")

# 한글·숫자·공백과 최소한의 문장부호만 허용. 다른 문자가 섞이면 오염이다.
BAD_CHAR = re.compile(r"[^가-힣ㄱ-ㅎㅏ-ㅣ0-9\s·,.()/~\-]")


def _tokens(text: str) -> list[str]:
    return [t for t in re.split(r"[\s,()]+", text) if t]


def symbol_in_visuals(symbol: str, visuals: list[str]) -> bool:
    """symbol 이 <item> 하나의 어절 부분수열인지 본다.

    프롬프트가 "수식을 덜어낸다"를 허용하므로 완전 일치를 요구할 수 없다.
    - "돌다리"            <= "강 위의 돌다리"             통과
    - "쏟아진 세 컵"       <= "쏟아진 세 개의 컵"          통과 (프롬프트의 O 예시)
    - "펜타클의 위치"      <= "펜타클의 위치(가슴 앞, ...)" 통과
    - "좁은 바위산 꼭대기"  <= "좁고 험한 바위산 꼭대기"     실패 (어절이 바뀜)
    """
    want = _tokens(symbol)
    for item in visuals:
        i = 0
        for tok in _tokens(item):
            if i < len(want) and want[i] == tok:
                i += 1
        if i == len(want):
            return True
    return False


def validate(card: dict, r: dict) -> list[str]:
    """카드 한 장의 기계 검증. generate() 와 check() 가 같은 규칙을 쓴다."""
    problems: list[str] = []
    symbols = r["symbols"]
    names = [s["symbol"] for s in symbols]

    # --- symbols ---
    if not 4 <= len(symbols) <= 5:
        problems.append(f"symbols {len(symbols)}개 (4~5)")
    if len(set(names)) != len(names):
        problems.append("symbol 중복")

    visuals = CARD_VISUALS.get(card["name"])
    for s in symbols:
        if len(s["symbol"]) > 15:
            problems.append(f"symbol {len(s['symbol'])}자 — \"{s['symbol']}\"")
        if len(s["meaning"]) > 25:
            problems.append(f"meaning {len(s['meaning'])}자 — \"{s['symbol']}\"")
        if visuals and not symbol_in_visuals(s["symbol"], visuals):
            problems.append(f"symbol 이 목록에 없음 — \"{s['symbol']}\"")

    # --- themes ---
    if not 3 <= len(r["themes"]) <= 5:
        problems.append(f"themes {len(r['themes'])}개 (3~5)")
    for t in r["themes"]:
        if len(_tokens(t)) > 2:
            problems.append(f"themes 가 문장에 가까움 — \"{t}\"")

    # --- 길이 ---
    for field in ("uprightEnergy", "reversedEnergy"):
        n = len(r[field])
        if n > 255:
            problems.append(f"{field} {n}자 — 컬럼 초과, 반드시 수정")
        elif n > 30:
            problems.append(f"{field} {n}자 — 30자 초과")
    if len(r["axis"]) > 255:
        problems.append(f"axis {len(r['axis'])}자 — 컬럼 초과")

    # --- 문자 오염 ---
    for field in ("uprightEnergy", "reversedEnergy"):
        bad = BAD_CHAR.findall(r[field])
        if bad:
            problems.append(f"{field} 에 한글 외 문자 {sorted(set(bad))}")
    for s in symbols:
        bad = BAD_CHAR.findall(s["symbol"]) + BAD_CHAR.findall(s["meaning"])
        if bad:
            problems.append(f"symbol/meaning 에 한글 외 문자 {sorted(set(bad))}")

    # --- axis ---
    if " vs " not in r["axis"]:
        problems.append("axis 가 'A vs B' 형태가 아님")
    if BAD_CHAR.findall(r["axis"].replace(" vs ", " ")):
        problems.append("axis 에 한글 외 문자")
    for ex in EXAMPLE_AXES:
        if difflib.SequenceMatcher(None, r["axis"], ex).ratio() >= 0.6:
            problems.append(f"axis 가 프롬프트 예시와 유사 — \"{ex}\"")

    # --- axisSupport ---
    sup = r["axisSupport"]
    a, b = sup["a"], sup["b"]
    if not a or not b:
        problems.append("axisSupport 한쪽이 비어 있음")
    overlap = set(a) & set(b)
    if overlap:
        problems.append(f"axisSupport a/b 중복 {sorted(overlap)}")
    unknown = (set(a) | set(b)) - set(names)
    if unknown:
        problems.append(f"axisSupport 가 symbols 에 없는 값을 참조 {sorted(unknown)}")
    if abs(len(a) - len(b)) > 2:
        problems.append(f"axisSupport 편중 a:{len(a)} b:{len(b)}")

    # --- reversedEnergy ---
    if any(w in r["reversedEnergy"] for w in ("하지 않", "가 없", "의 부재", "못함")):
        problems.append("reversedEnergy 가 단순 부정문일 수 있음")
    if any(w in r["reversedEnergy"] for w in RECOVERY_WORDS):
        problems.append("reversedEnergy 를 회복으로 뒤집었을 수 있음")

    # --- 컬럼 ---
    if len(card["name"]) > 50:
        problems.append(f"name {len(card['name'])}자 (컬럼 50)")

    return problems


def check(records: list[dict] | None = None) -> list[dict]:
    records = records if records is not None else load_all()
    if len(records) != 78:
        print(f"[경고] 78장 중 {len(records)}장만 있습니다.")

    failed = 0
    for r in records:
        problems = validate(r, r)
        if r["name"] in AXIS_REVIEW:
            problems.append(f"axis 눈으로 확인 → \"{r['axis']}\"")
        if problems:
            failed += 1
            print(f"[검토] {r['name']}: " + " / ".join(problems))

    # 카드 간 검사: 개별 호출은 서로를 모르므로 여기서만 잡을 수 있음
    by_axis: dict[str, list[str]] = {}
    for r in records:
        by_axis.setdefault(r["axis"].strip(), []).append(r["name"])
    for t, names in by_axis.items():
        if len(names) > 1:
            print(f"[중복] axis 동일 {names} → \"{t}\"")

    minors = [r for r in records if r["arcana"] == "MINOR"]
    for i, x in enumerate(minors):
        for y in minors[i + 1:]:
            if x["card_number"] != y["card_number"]:
                continue
            common = set(x["themes"]) & set(y["themes"])
            if len(common) >= 3:
                print(f"[중복] {x['name']} / {y['name']} themes 겹침 {sorted(common)}")

    # 눈으로 볼 것만 따로 뽑아 둔다: a 에 배정된 symbol 의 meaning 이 정말 A 쪽으로 기우는가
    review = []
    for r in records:
        by_name = {s["symbol"]: s["meaning"] for s in r["symbols"]}
        a, b = [s.strip() for s in r["axis"].split(" vs ", 1)] if " vs " in r["axis"] else ("?", "?")
        review.append({
            "name": r["name"],
            "A": a,
            "A_근거": {n: by_name.get(n, "??") for n in r["axisSupport"]["a"]},
            "B": b,
            "B_근거": {n: by_name.get(n, "??") for n in r["axisSupport"]["b"]},
        })
    REVIEW_PATH.write_text(json.dumps(review, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"검증 완료: {len(records)}장, 검토 필요 {failed}장 → {REVIEW_PATH}")
    return records


# --------------------------------------------------------------
# 5. seed SQL 출력
# --------------------------------------------------------------

def esc(value: str) -> str:
    """MySQL 문자열 리터럴 이스케이프 (백슬래시가 이스케이프 문자인 기본 모드 기준)."""
    return value.replace("\\", "\\\\").replace("'", "''")

def with_pole(record: dict) -> list[dict]:
    """axisSupport 의 a/b 를 각 symbol 에 pole 로 병합한다."""
    pole_of = {}
    for name in record["axisSupport"]["a"]:
        pole_of[name] = "a"
    for name in record["axisSupport"]["b"]:
        pole_of[name] = "b"

    merged = []
    for s in record["symbols"]:
        item = {"symbol": s["symbol"], "meaning": s["meaning"]}
        pole = pole_of.get(s["symbol"])
        if pole:
            item["pole"] = pole
        merged.append(item)
    return merged

def to_sql() -> None:
    records = check()
    if not records:
        print("생성된 JSON 이 없습니다. 먼저 generate 를 실행하세요.")
        return

    rows = []
    for r in records:
        symbols = esc(json.dumps(with_pole(r), ensure_ascii=False))
        themes = esc(json.dumps(r["themes"], ensure_ascii=False))
        rows.append(
            "({id}, '{name}', '{name_ko}', '{arcana}', '{suit}', {number}, "
            "'{symbols}', '{themes}', '{upright}', '{reversed}', '{axis}')".format(
                id=r["id"],
                name=esc(r["name"]),
                name_ko=esc(r["name_ko"]),
                arcana=r["arcana"],
                suit=r["suit"],
                number=r["card_number"],
                symbols=symbols,
                themes=themes,
                upright=esc(r["uprightEnergy"]),
                reversed=esc(r["reversedEnergy"]),
                axis=esc(r["axis"]),
            )
        )

    sql = (
        "-- 타로 78장 시드 데이터 (seed_cards.py 로 생성)\n"
        "INSERT INTO card\n"
        "    (id, name, name_ko, arcana, suit, card_number,\n"
        "     symbols, themes, upright_energy, reversed_energy, axis)\n"
        "VALUES\n"
        + ",\n".join(rows)
        + ";\n"
    )
    SQL_PATH.write_text(sql, encoding="utf-8")
    print(f"{SQL_PATH} 생성 완료 ({len(rows)}행)")


if __name__ == "__main__":
    command = sys.argv[1] if len(sys.argv) > 1 else "generate"
    if command == "generate":
        generate()
    elif command == "check":
        check()
    elif command == "sql":
        to_sql()
    else:
        print(__doc__)