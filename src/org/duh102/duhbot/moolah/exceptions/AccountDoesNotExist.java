class AccountDoesNotExist extends Exception {
  public AccountDoesNotExist() {}

  public AccountDoesNotExist(String message) {
    super(message);
  }
}
