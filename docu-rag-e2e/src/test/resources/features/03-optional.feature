Feature: Optional or not-yet-implemented API

  @optional @api
  Scenario: RAG conversation history returns 404 until implemented
    When I request RAG history for id "00000000-0000-0000-0000-000000000001"
    Then the HTTP status is 404
