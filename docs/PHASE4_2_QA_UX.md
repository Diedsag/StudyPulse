StudyPulse Phase 4.2 Q&A UX repair

Goal

Make Q&A screen not look silently broken.

Current reality

The existing Q&A is not a full RAG system. It is closer to keyword search over material chunks/text.

Therefore, no-answer cases can be technically expected, but product UX must explain them.

What changed

QaScreen now shows:

- a static demo notice explaining that Q&A uses keyword search;
- a dynamic status block that tries to inspect the current Q&A state and show:
  - loading state;
  - error state;
  - no-answer state.

The dynamic inspector uses reflection over the existing state object, so it does not require knowing exact ViewModel field names at patch time.

Expected user behavior

If answer is found:
- existing answer UI remains visible.

If answer is not found:
- user sees an explanation that keyword matches may be absent.

If request is loading:
- user sees loading message.

If error happens:
- user sees error message.

Out of scope

This phase does not implement:
- vector search;
- embeddings;
- cloud LLM;
- true RAG.

Those belong to later AI/RAG phases.