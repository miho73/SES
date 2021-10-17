# Standard of IPUAC

IPUAC는 IPU에서 문제등의 작성에 사용하는 문법입니다.

## 0. 지시문

지시문은 IPUAC를 처리하는 방식을 지시합니다. 모든 지시문은 `#`로 시작합니다.

* ### 정의

    `#def [속성명]=[값]`  
    문서 인터프리팅 중 사용할 속성을 정의합니다. 예를 들어 다음 명령은 `text-align` 속성의 값을 1로 정합니다.  
    `#def text-align=1`  
    정의는 문서의 어디에 았어도 좋습니다. 문서 도중에 정의로 속성의 값이 바뀐 경우 그 다음줄부터 변경사항이 적용됩니다.

## 1. 글 구조화

* ### 섹션

    `== 섹션명 ==`  
    문제 내용, 답, 해설 등 섹션을 나눌 때 사용합니다. 이 명령은 텍스트 정렬과 함께 쓰일 수 없습니다.

* ### 텍스트 정렬

    `\1`, `\2`, `\3`, `\4`  
    위 4가지 문자중 하나를 매 줄의 가장 첫 줄에 넣으세요. 각각이 의미하는 바는 다음과 같습니다.
    |입력|정렬|
    |-|-|
    |`\1`|왼쪽|
    |`\2`|가운데|
    |`\3`|오른쪽|
    |`\4`|양쪽에 맞추기|

    만약 전처리기에서 `text-align`의 값이 정의된 경우 텍스트 정렬이 정해지지 않은 줄의 텍스트 정렬은 속성을 따릅니다. 전처리기의 속성에는 1부터 4까지의 값이 들어가며, 각각의 의미는 위의 표와 같습니다.

## 2. 글

* ### 스타일

  |입력|출력|설명|
  |-|-|-|
  |''굵게''|**굵게**|작은 따옴표 2개|
  |'''기울임'''|*기울임*|작은 따옴표 3개|
  |''''굵게 기울임''''|***굵게 기울임***|작은 따옴표 4개|
  |\_\_밑줄\_\_|<u>밑줄</u>|언더바 2개|
  |--취소선--|~~취소선~~|빼기 기호 2개|
  |a^^b^^|a<sup>b</sup>|첨자의 텍스트 크기는 일부 작아집니다.|
  |a,,b,,|a<sub>b</sub>|첨자의 텍스트 크기는 일부 작아집니다.|

* ### 텍스트 크기

    텍스트 크기는 정수를 사용합니다. 이때 영이나 양의 정수더라도 부호를 생략할 수 없습니다.  
    `{{{크기 텍스트}}}`  
    실제 텍스트 크기는 다음과 같이 계산합니다.  
    `크기=1.1+0.1(크기 숫자)`  
    예를 들어 다음은 크기가 0.5em인 "Hello"를 보여줍니다.
    `{{{-6 Hello}}}`

* ### 텍스트 색

    CSS 색상 표기법을 사용합니다.  
    `{{{색상 텍스트}}}`  
    예를 들어 다음은 빨간색 "Hello"를 보여줍니다.  
    `{{{#f00 Hello}}}`

* ### 텍스트 배경색

    CSS 색상 표기법을 사용합니다.  
    `[[[색상 텍스트]]]`  
    예를 들어 다음은 빨간색 배경 위의 흰색 "Hello"를 보여줍니다.  
    `[[[#f00 {{{#fff Hello}}}]]]`

* ### 링크
  
    다음 방법으로 링크를 보여줄 수 있습니다.
    |입력|출력|
    |-|-|
    |[[URL]]|[URL](URL)|크
    |[[URL\|Go to URL]]|[Go to URL](URL)|

* ### 이미지 표시

    다음 명령은 이미지를 지정한 URL, 또는 경로에서 가져옵니다.
    |입력|출력|설명|
    |-|-|-|
    |[{img:이미지 코드}]|지정한 이미지|IPU 리소스 센터에서 이미지를 가져왑니다.|
    |[{이미지 URL}]|지정한 이미지|지정한 URL에서 이미지를 받아옵니다.|
    |[{img:이미지 코드\|매개변수}]|지정한 이미지|매개변수에는 HTML `img` 태그의 `alt`, `width`, `height`등의 값이 속성=값의 형태로 &로 구분되어 들어갑니다. 예를 들어 `[[img: abcd\|alt="이미지"&width="500"]]`입니다. 이 속성은 외부 이미지를 사용할 때도 그대로 사용 가능합니다.|

* ### 외부 참조

    다음 명령을 통해 외부 웹페이지를 임베딩할 수 있습니다.
    |입력|설명|
    |-|-|
    |[ytp(jNQXAC9IVRw)]|YouTube에서 해당 재생 ID를 가진 동영상을 재생합니다.|
    |[embed(URL)]|URL의 웹페이지를 임베딩하여 추가합니다.|
    |[embed(URL,매개변수)]|매개변수는 속성=값의 형태로 콤마로 구분됩니다. 이 구문은 YouTube 임베딩에서도 사용할 수 있습니다.|

* ### 인용

    `! 인용문`  
    인용문을 입력합니다. 여러 줄로 인용문을 작성하려면 여러줄에 작성하면 됩니다.

    ```md
    ! 인용1

    ! 인용2
    !
    ! 인용3
    ```

    > 인용1

    > 인용2
    >
    > 인용3

* ### 테이블

    `||`  
    셀의 각 칸을 나눕니다.

    ```md
    ||1||2||3||
    ||4||5||6||
    ```

    위 명령은 아래와 같은 테이블을 만듭니다.
    |1|2|3|
    |-|-|-|
    |4|5|6|

    셀 내에서 개행을 해야 할 시 매크로를 사용합니다.

    ```md
    ||셀 안에서 개행을 할 때는[lf]매크로를 사용합니다.||
    ```

    셀을 합치려면 각 셀의 가장 앞에 (-합칠 셀 수) 또는 (|합칠 셀 수)를 입력합니다. -는 가로방향으로, |는 세로방향으로 합칩니다.

    ```md
    ||(-4)1||
    ||(-2)1||(-2)1||
    ||1||2||2||1||
    ```

    셀에서의 텍스트 정렬은 지시문으로 합니다. 정의 `table-align`의 값에 따라 테이블 셀 속의 텍스트가 정렬됩니다.

* ### 수식

    `[math(LaTeX)]`  
    LaTeX 구문으로 수식을 입력합니다.

## 3. 매크로

* ### 줄바꿈

    `[lf]`

* ### 가림
  
    `[hide(대체 텍스트,내용)]`  
    내용을 가리는데 사용합니다. 대체 텍스트를 눌러야 내용을 볼 수 있습니다. 힌트 섹션에서 사용합니다. 예를 들어 다음은 '보기' 버튼을 눌러야 내용을 볼 수 있는 가림입니다.  
    `[hide(보기,가릴 내용)]`

## 4. 기타

* ## 구분선

    `---`  
    구분선을 삽입힙니다.