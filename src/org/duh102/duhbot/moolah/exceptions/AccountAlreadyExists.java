class AccountAlreadyExists extends Exception {
  public AccountAlreadyExists() {}

  public AccountAlreadyExists(String message) {
    super(message);
  }
}
