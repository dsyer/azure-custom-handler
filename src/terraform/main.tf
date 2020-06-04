provider "azurerm" {
  version = "~> 2.1"
  features {}
}

resource "azurerm_resource_group" "main" {
  name     = var.AZ_RESOURCE_GROUP
  location = var.AZ_LOCATION
}

resource "azurerm_storage_account" "main" {
  name                     = var.AZ_STORAGE_NAME
  resource_group_name      = azurerm_resource_group.main.name
  location                 = azurerm_resource_group.main.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
}

resource "azurerm_app_service_plan" "main" {
  name                = "${var.AZ_FUNCTION_NAME_APP}-plan"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  kind                = "Linux"
  reserved            = true
  sku {
    tier = "Elastic Premium"
    size = "EP1"
  }
}

resource "azurerm_function_app" "main" {
  name                       = var.AZ_FUNCTION_NAME_APP
  location                   = azurerm_resource_group.main.location
  resource_group_name        = azurerm_resource_group.main.name
  app_service_plan_id        = azurerm_app_service_plan.main.id
  storage_account_name       = azurerm_storage_account.main.name
  storage_account_access_key = azurerm_storage_account.main.primary_access_key
  version = "~3"
  app_settings = {
    WEBSITE_MOUNT_ENABLED = "1"
  }
}
