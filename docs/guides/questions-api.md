# Questions API

Show a player a popup window — a confirmation, a form, a multi-button menu —
by implementing one interface and sending raw BML markup. This is the
**low-level** path, mirroring Wurm's own `Question` system.

> **Pick the right tool first.** If your popup is "show some text and
> buttons" or even a multi-page wizard, the high-level
> [`ui-api.md`](ui-api.md) (`UIWindow.builder().addText(...).addButton(...)`)
> gets you there in ~30 lines without touching BML. Reach for ModQuestion
> only when you need finer control over the markup, or when you're porting
> a vanilla `Question` subclass and want to keep its layout intact.

---

## How a question works

The flow is one round-trip:

```
[your code]                          [server]                 [player]
   │                                    │                        │
   ├─ ModQuestions.createQuestion(…) ──►│                        │
   ├─ question.sendQuestion() ──────────┼──── BML window ───────►│
   │                                    │                        │
   │                                    │◄── form Properties ────┤
   │◄── answer(question, properties) ───┤                        │
```

You implement two methods on `ModQuestion`:

- **`sendQuestion(Question)`** — build BML, call `sendBml(...)` to push the
  window to the player.
- **`answer(Question, Properties)`** — handle the response. The `Properties`
  map is keyed by the `id=` attributes in your BML inputs/buttons.

The framework wires the round-trip; you never poll for the answer.

---

## Minimal example: a yes/no confirmation

```java
package com.example.mymod.questions;

import com.garward.wurmmodloader.modsupport.questions.ModQuestion;
import com.garward.wurmmodloader.modsupport.questions.ModQuestions;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.Question;

import java.util.Properties;

public class ConfirmDeleteQuestion implements ModQuestion {

    private final Runnable onConfirm;

    public ConfirmDeleteQuestion(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    public static void show(Creature player, Runnable onConfirm) {
        Question q = ModQuestions.createQuestion(
                player,
                "Confirm",                              // window title
                "Really delete this thing?",            // question text
                player.getWurmId(),                     // target id (anything you want to echo back)
                new ConfirmDeleteQuestion(onConfirm));
        q.sendQuestion();
    }

    @Override
    public void sendQuestion(Question question) {
        StringBuilder bml = new StringBuilder();
        bml.append("text{text='Really delete this thing?'}");
        bml.append("harray{");
        bml.append("  button{id='yes';text='Delete'}");
        bml.append("  button{id='cancel';text='Cancel'}");
        bml.append("}");
        sendBml(question, 260, 120, false, true, bml);
    }

    @Override
    public void answer(Question question, Properties answers) {
        if ("true".equals(answers.getProperty("yes"))) {
            onConfirm.run();
        }
        // "cancel" → do nothing; window already closed
    }
}
```

Call site (e.g. from a context-menu handler):

```java
ConfirmDeleteQuestion.show(player, () -> deleteItem(item));
```

That's the whole pattern. The button `id='yes'` becomes
`answers.getProperty("yes") == "true"` when the player clicks it. Same for
input fields, dropdowns, and checkboxes — every BML element with an `id`
maps into the returned `Properties`.

---

## The `Properties` you get back

| BML element | Key | Value |
|---|---|---|
| `button{id='X'}` | `X` | `"true"` if clicked |
| `input{id='X'}` | `X` | the entered text |
| `passwordinput{id='X'}` | `X` | the entered text |
| `dropdown{id='X';options='…'}` | `X` | index of the selected option (`"0"`, `"1"`, …) |
| `checkbox{id='X'}` | `X` | `"true"` / `"false"` |
| `radio{id='X';group='G'}` | `G` | `id` of the selected radio in the group |

Always null-check `answers.getProperty(...)` — players can close the window
without picking anything.

---

## Beyond minimal: `ModQuestions` helpers

`ModQuestions` is a static facade over private methods on Wurm's `Question`
class. You only need it when you want to look like a vanilla popup —
matching headers, scroll regions, and standard answer buttons.

| Helper | Returns |
|---|---|
| `getBmlHeader(q)` | Standard question header (title bar + question text) |
| `getBmlHeaderNoQuestion(q)` | Header without the question line |
| `getBmlHeaderWithScroll(q)` | Header that wraps content in a scrollable region — use for long forms |
| `getBmlHeaderWithScrollAndQuestion(q)` | Both: scroll region + question line |
| `getBmlHeaderScrollOnly(q)` | Just the scroll wrapper, no header |
| `createOkAnswerButton(q)` | "OK" button matching vanilla styling |
| `createBackAnswerButton(q)` | "Back" button (for paged flows) |
| `createAnswerButton2(q)` / `createAnswerButton2(q, "Custom")` | Generic answer button — default text or custom |
| `createAnswerButton3(q)` | Alternate styling |
| `createAnswerButtonForNoBorder(q)` | Borderless variant |

Typical scrolling form:

```java
@Override
public void sendQuestion(Question question) {
    StringBuilder bml = new StringBuilder();
    bml.append(ModQuestions.getBmlHeaderWithScrollAndQuestion(question));
    bml.append("text{text='Pick a target:'}");
    bml.append("dropdown{id='target';options='Player A,Player B,Player C'}");
    bml.append("input{id='reason';maxchars='200'}");
    bml.append(ModQuestions.createOkAnswerButton(question));
    sendBml(question, 400, 300, true, true, bml);
}
```

---

## `sendBml` parameters

```java
sendBml(question, width, height, resizeable, closeable, content);
sendBml(question, width, height, resizeable, closeable, content,
        red, green, blue, title);  // overrides window color + title
```

| Param | Notes |
|---|---|
| `width` / `height` | Pixels. Forms need ~80–120px per row of inputs; buttons want ≥250px wide so labels don't wrap. |
| `resizeable` | Almost always `true` — players resize windows constantly. |
| `closeable` | `false` for forced choices (e.g. EULA accept). Use sparingly — players hate trapped windows. |
| `red/green/blue` | Window chrome tint, 0–255. The default `(200,200,200)` matches vanilla. |
| `title` | Defaults to `question.getTitle()` if you use the short overload. |

---

## BML syntax cheats

The full BML reference is in [`bml-ui.md`](bml-ui.md). The two rules that
trip everyone up:

- **No semicolons between top-level appends** — `text{...}` `text{...}`,
  not `text{...};text{...}`. Semicolons are only used **inside** containers
  (`table{rows='…';cols='…';text{...};text{...}}`).
- **No spaces around `=`** — `text='value'`, not `text = 'value'`.

Quick element reference:

```
text{text='Hello'}                          // plain line
text{text='Bold';type='bold'}               // styled line
input{id='name';maxchars='50'}              // single-line text input
passwordinput{id='pwd';maxchars='32'}       // masked input
dropdown{id='choice';options='A,B,C'}       // dropdown
checkbox{id='agree';text='I agree'}         // checkbox with label
button{id='submit';text='Submit'}           // button
harray{ … }                                 // arrange children horizontally
varray{ … }                                 // arrange children vertically
table{rows='2';cols='3';  cell;cell;cell;  cell;cell;cell }
```

---

## When to use ModQuestion vs the UIWindow API

| You want… | Use |
|---|---|
| One-shot confirmation / yes-no popup | UIWindow API ([`ui-api.md`](ui-api.md)) |
| Multi-page wizard, dropdowns, dynamic state | UIWindow API |
| Pixel-precise vanilla-looking popup matching a specific Wurm window | ModQuestion + `ModQuestions` helpers |
| Porting an existing `org.gotti.*` `Question` subclass | ModQuestion (same shape) |
| You need scrolling regions or table layouts BML supports natively | ModQuestion |

The two APIs coexist — a single mod can use ModQuestion for one window and
UIWindow for another without conflict.

---

## Real-world examples

- **`examples/templatemod/QuestionnaireExample.java`** — multi-page
  questionnaire built with the **UIWindow API** (no BML). Read this first
  if you haven't decided which path to take — it's almost certainly enough.
- Mod source trees with custom ModQuestion subclasses are scattered across
  `mods/` — find them with:

  ```bash
  grep -rln "extends ModQuestion" mods/
  ```

---

## Common pitfalls

- **Forgetting `id=` on a button.** No id → no key in `Properties` → your
  `answer()` can't tell what was clicked. Every interactive element needs an
  id.
- **Treating button values as booleans.** They come back as the *string*
  `"true"`, not a boolean. Use `"true".equals(answers.getProperty("X"))`.
- **Sending an unclosed BML element.** A missing `}` won't error loudly —
  the window just renders empty or partial. If your popup looks broken,
  count braces.
- **Window too small for content.** BML doesn't auto-size. Compute height
  roughly as `header(60) + rows(80–120 each) + buttons(60)`.
- **Building BML across multiple `sendBml()` calls.** Each `sendBml`
  replaces the window. Build one `StringBuilder`, send once.

---

## See also

- **[`ui-api.md`](ui-api.md)** — high-level `UIWindow.builder()` API; the
  default choice for new popups
- **[`bml-ui.md`](bml-ui.md)** — full BML syntax reference (elements,
  attributes, layout containers)
- **[`ui-api-overview.md`](ui-api-overview.md)** / **[`ui-api-submenus.md`](ui-api-submenus.md)** —
  context-menu integration: how to make a popup appear when the player
  right-clicks
- **[`event-bus.md`](event-bus.md)** — most popups are triggered from an
  event handler (`PlayerLoginEvent`, `BodyMenuPopulateEvent`, etc.)
- **Source:** [`wurmmodloader-modsupport/.../questions/`](../../wurmmodloader-modsupport/src/main/java/com/garward/wurmmodloader/modsupport/questions/)
  — `ModQuestion`, `ModQuestions`
